import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

fun main() {
    val input = ""
August 2026
01-08-2026 (Sat) :
Suresh returned +1000
Nanna -8000
Sunil pg rent 3000
Train (hdp to hyd) 472.5
Ice Cream 50
Hair cutting 150
Tea 40
Egg puff 100

02-08-2026 (Sun) : egga 51

03-08-2026 (Mon) :
Bus 40 

10-08-2026 (Mon) : power recharge 200
Biryani 153 +155

26-08-2026 (Wed) :
Vijay bro lend -200

30-08-2026 (Sun) : bus 15+30
Church 100 
Dinesh popcorn 15
Ice Cream me and Dinesh 80
Dinesh biryani 150
Dinesh paid mine ( pant(799), mug(29), soap(50)) 879 
Need to return the amount 879 - amount I spend for him(335) = 554
Curd 10
Chandh power share 50

31-08-2026 (Mon) : tablets 35
""

    val dateRegex = Regex("^(\\d{2}-\\d{2}-\\d{4})\\s*\\([A-Za-z]{3}\\)\\s*:(.*)$")
    val amountRegex = Regex("^(.*?)\\s+([+-]?\\s*\\d+(?:\\.\\d+)?(?:\\s*\\+\\s*\\d+(?:\\.\\d+)?)*)$")
    
    var currentDate = ""
    
    input.lines().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("August")) return@forEach
        
        val dateMatch = dateRegex.find(line)
        if (dateMatch != null) {
            currentDate = dateMatch.groupValues[1]
            val remainder = dateMatch.groupValues[2].trim()
            if (remainder.isNotEmpty()) {
                parseTransaction(remainder, currentDate, amountRegex)
            }
        } else {
            parseTransaction(line, currentDate, amountRegex)
        }
    }
}

fun parseTransaction(line: String, date: String, amountRegex: Regex) {
    val match = amountRegex.find(line)
    if (match != null) {
        val desc = match.groupValues[1].trim()
        val amountStr = match.groupValues[2].replace(" ", "")
        var isIncome = false
        var finalAmountStr = amountStr
        
        if (amountStr.startsWith("+")) {
            isIncome = true
            finalAmountStr = amountStr.substring(1)
        } else if (amountStr.startsWith("-")) {
            isIncome = false
            finalAmountStr = amountStr.substring(1)
        } else if (desc.contains("returned", ignoreCase = true) || desc.contains("share", ignoreCase = true) && !desc.contains("power")) {
            // maybe income? The prompt had "+1000" and "+200" for income.
        }
        
        // calculate sum if there's a plus
        val parts = finalAmountStr.split("+")
        var totalAmount = 0.0
        try {
            totalAmount = parts.sumOf { it.toDouble() }
        } catch(e: Exception) {
            println("Error parsing amount: ")
        }
        
        val type = if(isIncome) "INCOME" else "EXPENSE"
        println("[]  |  | ")
    } else {
        println("Could not parse: ")
    }
}
main()
