package de.plapadoo.bbdbparser

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

data class BbdbPhone(
        val identifier: String,
        val number: String)

data class BbdbAddress(
        val identifier: String,
        val streets: List<String>,
        val street3: String,
        val city: String,
        val state: String,
        val zip: String,
        val country: String)

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
    class BbdbRawCollection(val objects: List<BbdbRawObject>) : BbdbRawObject()
    class BbdbRawString(val value: String) : BbdbRawObject()
    class BbdbRawInteger(val value: Int) : BbdbRawObject()
    class BbdbRawNil() : BbdbRawObject()
    class BbdbRawList(val value: List<BbdbRawCollection>) : BbdbRawObject()
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

    override fun visitBbdbList(ctx: BbdbParser.BbdbListContext?): BbdbRawObject {
        return BbdbRawObject.BbdbRawList(value = ctx!!.vector().map { visitVector(it) })
    }
}

fun <T>arrayGetSafe(collection : List<T>,index : Int) : T? {
    return if (collection.size < index) collection[index] else null
}

fun rawToRealPhone(raw : BbdbRawObject.BbdbRawCollection) :BbdbPhone{

}

fun rawToRealAddress(raw : BbdbRawObject.BbdbRawCollection) :BbdbAddress{

}

fun rawToRealEntry(raw : BbdbRawObject.BbdbRawCollection) :BbdbEntry{
    return BbdbEntry(
            firstName = arrayGetSafe(raw.objects,0).let { (it as BbdbRawObject.BbdbRawString).value },
            lastName = arrayGetSafe(raw.objects,1).let { (it as BbdbRawObject.BbdbRawString).value },
            akas = arrayGetSafe(raw.objects,2).let { (it as BbdbRawObject.BbdbRawCollection).objects.map { (it as BbdbRawObject.BbdbRawString).value } },
            company = arrayGetSafe(raw.objects,3).let { (it as BbdbRawObject.BbdbRawString).value },
            phones = arrayGetSafe(raw.objects,4).let { (it as BbdbRawObject.BbdbRawCollection).objects.map { rawToRealPhone(it as BbdbRawObject.BbdbRawCollection) } },
            addresses = arrayGetSafe(raw.objects,5).let { (it as BbdbRawObject.BbdbRawCollection).objects.map { rawToRealAddress(it as BbdbRawObject.BbdbRawCollection) } },
            networkAddresses = arrayGetSafe(raw.objects,6).let { (it as BbdbRawObject.BbdbRawCollection).objects.map { (it as BbdbRawObject.BbdbRawString).value } },
            notes = arrayGetSafe(raw.objects,7).let { (it as BbdbRawObject.BbdbRawAList).associations.associate { Pair(it.key,(it.value as BbdbRawObject.BbdbRawString).value) }})
}

fun rawToReal(raw : BbdbRawObject.BbdbRawList) : List<BbdbEntry> {
    return raw.value.map { rawToRealEntry(it) }
}

fun parseBbdb(input: String): List<BbdbEntry> {
    val inputStream = ANTLRInputStream(input)
    val lexer = BbdbLexer(inputStream)
    val tokenStream = CommonTokenStream(lexer)
    val parser = BbdbParser(tokenStream)
    val bbdbListRaw = BbdbRawObjectVisitor().visitBbdbList(parser.bbdbList())

}
