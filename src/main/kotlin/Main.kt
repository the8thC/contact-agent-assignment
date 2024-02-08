
import com.google.ortools.Loader
import com.google.ortools.graph.MinCostFlow
import com.google.ortools.graph.MinCostFlowBase.Status.OPTIMAL


data class Contact(
    val requirement: String,
    var index: Int = -1,
    var connected: Boolean = false)
data class Agent(
    val requirements: String,
    var index: Int = -1,
    var connected: Boolean = false) {

    fun contains(contact: Contact) : Boolean {
        return requirements.contains(contact.requirement)
    }
}

fun main(args: Array<String>) {
    Loader.loadNativeLibraries()

    val agents = listOf(
        Agent("ACE"),
        Agent("BCD"),
        Agent("AB"),
        Agent("AC"),
    )
    val contacts = listOf(
        Contact("A"),
        Contact("B"),
        Contact("C"),
        Contact("D"),
        Contact("E"),
    )

    solveWithGoogleOR(agents, contacts)
}

fun solveWithGoogleOR(agents: List<Agent>, contacts: List<Contact>) {
    var currIndex = 0
    val sourceIndex = currIndex++
    for (agent in agents) agent.index = currIndex++
    for (contact in contacts) contact.index = currIndex++

    val targetIndex = currIndex
    val capacity = 1L

    // Instantiate a SimpleMinCostFlow solver.
    val minCostFlow = MinCostFlow(targetIndex + 1)

    // add arcs from agents to contacts
    for (agent in agents) {
        for (contact in contacts.filter { agent.contains(it) }) {
            minCostFlow.addArcWithCapacityAndUnitCost(
                agent.index, contact.index, capacity, contacts.indexOf(contact) + 1L
            )
            agent.connected = true
            contact.connected = true
        }
    }
    // add arcs to agents
    for (agent in agents) {
        if (!agent.connected)
            continue
        minCostFlow.addArcWithCapacityAndUnitCost(
            sourceIndex, agent.index, capacity, 0
        )
        // set agent supplies
        minCostFlow.setNodeSupply(agent.index, 0)
    }

    // add arcs from contacts to target
    for (contact in contacts) {
        if (!contact.connected)
            continue
        minCostFlow.addArcWithCapacityAndUnitCost(
            contact.index, targetIndex, capacity, 0
        )
        // set contact supplies
        minCostFlow.setNodeSupply(contact.index, 0)
    }

    val supply = minOf(
        agents.sumOf { if (it.connected) 1L else 0L },
        contacts.sumOf { if (it.connected) 1L else 0L }
    )
    // set initialSupply
    minCostFlow.setNodeSupply(sourceIndex, supply)
    // set target supply
    minCostFlow.setNodeSupply(targetIndex, -supply)

    // Find the min cost flow.
    val status = minCostFlow.solve()

    if (status == OPTIMAL) {
        println("Total cost: " + minCostFlow.optimalCost)
        println()
        for (i in 0..<minCostFlow.numArcs) {
            // Can ignore arcs leading out of source or into sink.
            if (minCostFlow.getTail(i) != sourceIndex && minCostFlow.getHead(i) != targetIndex) {
                // Arcs in the solution have a flow value of 1. Their start and end nodes
                // give an assignment of worker to task.
                if (minCostFlow.getFlow(i) > 0) {
                    println(
                        "Worker ${agents.find { it.index == minCostFlow.getTail(i) }} assigned to task " +
                                "${contacts.find { it.index == minCostFlow.getHead(i) }}" +
                                " Cost: " + minCostFlow.getUnitCost(i)
                    )
                }
            }
        }
    } else {
        println("Solving the min cost flow problem failed.")
        println("Solver status: $status")
    }
}
