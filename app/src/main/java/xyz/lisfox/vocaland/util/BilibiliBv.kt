package xyz.lisfox.vocaland.util

private const val BV_ALPHABET =
    "FcwAPNKTMug3GV5Lj7EJnHpWsx4tb8haYeviqBz6rkCy12mUSDQX9RdoZf"
private const val BV_XOR = 23442827791579L
private const val BV_MASK = 2251799813685247L

fun bvidToAid(bvid: String): Long {
    if (bvid.length != 12 || !bvid.startsWith("BV1")) return 0
    val chars = bvid.toCharArray()
    val char3 = chars[3]
    val char9 = chars[9]
    val char4 = chars[4]
    val char7 = chars[7]
    chars[3] = char9
    chars[9] = char3
    chars[4] = char7
    chars[7] = char4

    var value = 0L
    for (index in 3 until chars.size) {
        val digit = BV_ALPHABET.indexOf(chars[index])
        if (digit < 0) return 0
        value = value * 58 + digit
    }
    return (value and BV_MASK) xor BV_XOR
}
