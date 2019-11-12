import javax.enterprise.event.Event
import javax.enterprise.event.Observes
import javax.enterprise.inject.se.SeContainerInitializer
import javax.inject.Inject

fun main() {

    val serviceSets = listOf(
            arrayOf(PrintRequestService::class.java, PrintedNotificationService::class.java),
            arrayOf(PrintRequestService::class.java, PrintedNotificationService::class.java, RandomUnknownListenerOnPrintRequests::class.java)
    )

    // run the "same" thing on a different setup
    for (serviceSet in serviceSets) {
        // init CDI with some services
        val container = SeContainerInitializer.newInstance().disableDiscovery().addBeanClasses(*serviceSet).initialize()

        // resolve instance of first microservice
        val printRequestEventServiceInstance = container.select(PrintRequestService::class.java).get()
        // show that you can call it directly
        printRequestEventServiceInstance.printMessage("Hello via method!")

        // or you can fire event and let the system deal with it
        container.beanManager.fireEvent(PrintRequestEvent("Hello using event!"))
    }

}

// two data classes, each represents a method call in event notification model
data class PrintRequestEvent(val message: String)
data class MessagePrintedEvent(val message: String)

// first "microservice"
class PrintRequestService {

    // this is the actual implementation
    private fun printMessageInternal(message: String) {
        println("Instance $this is printing message: $message")
    }

    // connector for inline code
    // is coupled with the specific (== typed) followup consumer
    @Inject
    private lateinit var printedNotificationService: PrintedNotificationService
    fun printMessage(message: String) {
        printMessageInternal(message)
        // hardcode followup method. Although this should be done one
        // one layer higher
        printedNotificationService.notifyPrinted(message)
    }


    // connector on cdi event bus
    // subscribes to certain event type
    // loose coupling, has no idea who will react on the consequently fired event
    @Inject
    private lateinit var messagePrinted: Event<MessagePrintedEvent>
    private fun printRequestEvent(@Observes event: PrintRequestEvent) {
        printMessageInternal(event.message)
        messagePrinted.fire(MessagePrintedEvent(event.message))
    }

}

// second "microservice"
class PrintedNotificationService {

    // actual computing
    private fun notifyPrintedInternal(message: String) {
        println("I, $this , have been notified that message '$message' has been sent.")
    }

    // expose method to a coder
    fun notifyPrinted(message: String) {
        notifyPrintedInternal(message);
    }

    // subscribe to event type
    private fun messagePrintedEvent(@Observes event: MessagePrintedEvent) {
        notifyPrintedInternal(event.message);
    }

}

class RandomUnknownListenerOnPrintRequests {

    private fun printRequestEvent(@Observes event: PrintRequestEvent) {
        println("I am $this and I have been notified about $event")
    }

}