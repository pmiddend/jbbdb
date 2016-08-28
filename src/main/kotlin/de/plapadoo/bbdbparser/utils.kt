package de.plapadoo.bbdbparser


class Either<out Left, Right> private constructor(val left: Left?, val right: Right?) {
    companion object {
        fun <Left1, Right1> ofLeft(left: Left1): Either<Left1, Right1> {
            return Either(left = left, right = null)
        }

        fun <Left1, Right1> ofRight(right: Right1): Either<Left1, Right1> {
            return Either(left = null, right = right)
        }
    }

    fun <T> match(f : (Left) -> T,g : (Right) -> T) : T {
        return if (left != null) f(left) else g(right!!)
    }

    fun <Left2> mapLeft(f: (Left) -> Left2) : Either<Left2,Right> {
        return match({ l -> ofLeft(f(l))},{ right -> ofRight(right)})
    }

    override fun toString(): String{
        return match({"$left"},{"$right"})
    }
}
