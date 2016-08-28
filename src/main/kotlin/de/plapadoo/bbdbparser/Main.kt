package de.plapadoo.bbdbparser

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import java.io.FileReader

data class BbdbPhone(
        val identifier: String,
        val number: String)

data class BbdbAddress(
        val identifier: String?,
        val streets: List<String>,
        val street3: String?,
        val city: String?,
        val state: String?,
        val zip: String?,
        val country: String?)

data class BbdbEntry(
        val firstName: String?,
        val lastName: String?,
        val akas: List<String>,
        val company: String?,
        val phones: List<BbdbPhone>,
        val addresses: List<BbdbAddress>,
        val networkAddresses: List<String>,
        val notes: Map<String, String>)

sealed class BbdbRawObject {
    class BbdbRawAList(val associations: List<BbdbRawAListEntry>) : BbdbRawObject()
    class BbdbRawAListEntry(val key: String, val value: BbdbRawObject) : BbdbRawObject()
    class BbdbRawCollection(val objects: List<BbdbRawObject>) : BbdbRawObject() {
        fun get(i: Int): BbdbRawObject? {
            return objects[i]
        }
    }

    class BbdbRawString(val value: String) : BbdbRawObject()
    class BbdbRawInteger(val value: Int) : BbdbRawObject()
    class BbdbRawNil() : BbdbRawObject()
}

class BbdbRawObjectVisitor : BbdbBaseVisitor<BbdbRawObject>() {
    override fun visitAlist(ctx: BbdbParser.AlistContext?): BbdbRawObject {
        return BbdbRawObject.BbdbRawAList(associations = ctx!!.alistEntry().map { visitAlistEntry(it) })
    }

    override fun visitVector(ctx: BbdbParser.VectorContext?): BbdbRawObject.BbdbRawCollection {
        return BbdbRawObject.BbdbRawCollection(objects = ctx!!.`object`().map { visitObject(it) })
    }

    override fun visitList(ctx: BbdbParser.ListContext?): BbdbRawObject {
        return BbdbRawObject.BbdbRawCollection(objects = ctx!!.`object`().map { visitObject(it) })
    }

    override fun visitAlistEntry(ctx: BbdbParser.AlistEntryContext?): BbdbRawObject.BbdbRawAListEntry {
        return BbdbRawObject.BbdbRawAListEntry(key = ctx!!.lispIdentifier().text, value = visitObject(ctx.`object`()))
    }

    override fun visitObject(ctx: BbdbParser.ObjectContext?): BbdbRawObject {
        return if (ctx!!.nil() != null) BbdbRawObject.BbdbRawNil() else if (ctx.integer() != null) BbdbRawObject.BbdbRawInteger(Integer.parseInt(ctx.integer().text)) else if (ctx.string() != null) BbdbRawObject.BbdbRawString(ctx.string().text) else if (ctx.list() != null) visitList(ctx.list()) else if (ctx.vector() != null) visitVector(ctx.vector()) else if (ctx.alist() != null) visitAlist(ctx.alist()) else throw RuntimeException("invalid grammar")
    }
}

fun <T : BbdbRawObject> arrayGetSafe(collection: List<T>, index: Int): T? {
    return if (collection.size > index) collection[index].let { if (it is BbdbRawObject.BbdbRawNil) null else it } else null
}

fun toSimpleName(o : BbdbRawObject) : String{
    return when(o) {
        is BbdbRawObject.BbdbRawString -> "string"
        is BbdbRawObject.BbdbRawAList -> "association list"
        is BbdbRawObject.BbdbRawAListEntry -> "association list entry"
        is BbdbRawObject.BbdbRawCollection -> "list/vector"
        is BbdbRawObject.BbdbRawInteger -> "integer"
        is BbdbRawObject.BbdbRawNil -> "nil"
    }
}

fun <T : BbdbRawObject> arrayGetSafeString(collection: List<T>, index: Int): String? {
    val t = arrayGetSafe(collection, index) ?: return null
    if(t !is BbdbRawObject.BbdbRawString)
        throw RuntimeException("index $index: expected string, got ${toSimpleName(t)}")
    return t.let { (it as BbdbRawObject.BbdbRawString).value }
}

fun <T : BbdbRawObject> arrayGetSafeCollection(collection: List<T>, index: Int): List<BbdbRawObject> {
    val t = arrayGetSafe(collection, index) ?: return emptyList()
    if(t !is BbdbRawObject.BbdbRawCollection)
        throw RuntimeException("index $index: expected collection, got ${toSimpleName(t)}")
    return t.let { (it as BbdbRawObject.BbdbRawCollection).objects }
}

fun rawToRealPhone(raw: BbdbRawObject.BbdbRawCollection): BbdbPhone {
    return BbdbPhone(identifier = arrayGetSafeString(raw.objects, 0)!!, number = arrayGetSafeString(raw.objects, 1)!!)
}

fun rawToRealAddress(raw: BbdbRawObject.BbdbRawCollection): BbdbAddress {
    return BbdbAddress(
            identifier = arrayGetSafeString(raw.objects, 0),
            streets = arrayGetSafeCollection(raw.objects, 1).map { it as BbdbRawObject.BbdbRawString }.map { it.value },
            street3 = arrayGetSafeString(raw.objects, 2),
            city = arrayGetSafeString(raw.objects, 3),
            state = arrayGetSafeString(raw.objects, 4),
            zip = arrayGetSafeString(raw.objects, 5),
            country = arrayGetSafeString(raw.objects, 6))
}

fun rawToRealEntry(raw: BbdbRawObject.BbdbRawCollection): BbdbEntry {
    return BbdbEntry(
            firstName = arrayGetSafeString(raw.objects, 0),
            lastName = arrayGetSafeString(raw.objects, 1),
            akas = arrayGetSafeCollection(raw.objects, 4).map { (it as BbdbRawObject.BbdbRawString).value },
            company = arrayGetSafeString(raw.objects, 3),
            phones = arrayGetSafeCollection(raw.objects, 5).map { rawToRealPhone(it as BbdbRawObject.BbdbRawCollection) },
            addresses = arrayGetSafeCollection(raw.objects, 6).map { rawToRealAddress(it as BbdbRawObject.BbdbRawCollection) },
            networkAddresses = arrayGetSafeCollection(raw.objects, 7).map { (it as BbdbRawObject.BbdbRawString).value },
            notes = arrayGetSafe(raw.objects, 8)?.let { (it as BbdbRawObject.BbdbRawAList).associations.associate { Pair(it.key, (it.value as BbdbRawObject.BbdbRawString).value) } } ?: emptyMap())
}

data class LineError(val line: Int, val error: String) {
    override fun toString(): String {
        return "line $line: $error"
    }
}

fun parseBbdbLine(input: String): Either<String, BbdbEntry> {
    val inputStream = ANTLRInputStream(input)
    val lexer = BbdbLexer(inputStream)
    val tokenStream = CommonTokenStream(lexer)
    val parser = BbdbParser(tokenStream)
    val bbdbListRaw = BbdbRawObjectVisitor().visit(parser.vector())
    try {
        return Either.ofRight(rawToRealEntry(bbdbListRaw as BbdbRawObject.BbdbRawCollection))
    } catch (e: Exception) {
        return Either.ofLeft(e.message ?: "unknown error ${e.javaClass.simpleName}")
    }
}

fun main(args: Array<String>) {
    FileReader("/home/philipp/notes/bbdb").readLines().mapIndexed { i, s -> if (s.startsWith(';')) Either.ofLeft(LineError(i+1, "comment")) else parseBbdbLine(s).mapLeft { LineError(i+1, it) } }.forEach { println(it) }
}
