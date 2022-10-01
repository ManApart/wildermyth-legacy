import Personality.*

//https://wildermyth.com/wiki/Talk:Relationship
//The first three are positive, the last two are negative
enum class CompatibilityStats(vararg val others: Personality) {
    bookish(BOOKISH, HEALER, SNARK, GREEDY, GOOFBALL),
    coward(COWARD, LEADER, HEALER, HOTHEAD, LONER),
    goofball(GOOFBALL, GREEDY, SNARK, BOOKISH, POET),
    healer(HEALER, COWARD, BOOKISH, HOTHEAD, ROMANTIC),
    hothead(HOTHEAD, LEADER, ROMANTIC, HEALER, COWARD),
    leader(LEADER, COWARD, HOTHEAD, LONER, SNARK),
    loner(LONER, ROMANTIC, POET, COWARD, LEADER),
    greedy(GREEDY, POET, GOOFBALL, ROMANTIC, BOOKISH),
    poet(POET, LONER, GREEDY, SNARK, GOOFBALL),
    romantic(ROMANTIC, LONER, HOTHEAD, HEALER, GREEDY),
    snark(SNARK, BOOKISH, GOOFBALL, LEADER, POET);

}
    fun getCompatibility(self: Personality, others: List<Personality>): Int {
        val base = CompatibilityStats.valueOf(self.name.lowercase()).others.toList()
        val positive = base.subList(0, 3)
        val negative = base.subList(3, 5)
        return others.count { positive.contains(it) } - others.count { negative.contains(it) }
    }
