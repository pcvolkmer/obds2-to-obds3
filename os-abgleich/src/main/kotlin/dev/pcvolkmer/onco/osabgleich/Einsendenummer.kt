package dev.pcvolkmer.onco.osabgleich

class Einsendenummer private constructor(private val value: String) {

    override fun equals(other: Any?): Boolean {
        return other is Einsendenummer && other.value == value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value
    }

    companion object {
        fun from(value: String): Einsendenummer {
            var result = Einsendenummer(value)

            "^([A-Za-z])/20(\\d{2})/(\\d+)(\\.\\d+)?$".toRegex().find(value)?.let { matchResult ->
                val (letter, year, number, _) = matchResult.destructured
                result = Einsendenummer("$letter/20$year/${number.padStart(6, '0')}")
            }

            "^([A-Za-z])/(\\d{2})/(\\d+)(\\.\\d+)?$".toRegex().find(value)?.let { matchResult ->
                val (letter, year, number, _) = matchResult.destructured
                result = Einsendenummer("$letter/20$year/${number.padStart(6, '0')}")
            }

            "^([A-Za-z])(\\d{2})/(\\d+)(\\.\\d+)?$".toRegex().find(value)?.let { matchResult ->
                val (letter, year, number, _) = matchResult.destructured
                result = Einsendenummer("$letter/20$year/${number.padStart(6, '0')}")
            }

            return result
        }
    }

}