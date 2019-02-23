package den.ptrq.stpete.interaction

import den.ptrq.stpete.forecast.Forecast
import den.ptrq.stpete.forecast.ForecastDao
import den.ptrq.stpete.notification.NotificationSender
import den.ptrq.stpete.subscription.Subscription
import den.ptrq.stpete.subscription.SubscriptionDao
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * @author petrique
 */
class InteractionProcessor(
    private val notificationSender: NotificationSender,
    private val transactionTemplate: TransactionTemplate,
    private val interactionDao: InteractionDao,
    private val subscriptionDao: SubscriptionDao,
    private val forecastDao: ForecastDao
) {

    @Scheduled(fixedRate = 10000, initialDelay = 10000)
    fun processInteractions() {
        log.info("processing interactions")
        interactionDao.selectUnprocessed(limit = 3).forEach { process(it) }
    }

    private fun process(interaction: Interaction) {
        log.info("process({})", interaction)

        val isStartCommandPresent = interaction.keyWords.asSequence()
            .filter { it.type == KeyWord.Type.BOT_COMMAND }
            .any { it.value == "start" }

        var subscription: Subscription? = null
        transactionTemplate.execute {
            interactionDao.markAsProcessed(interaction)
            if (isStartCommandPresent) {
                subscription = createSubscription(interaction)
                subscriptionDao.insert(subscription!!)
            }
        }

        if (subscription != null) {
            val forecastList = forecastDao.getActual().asSequence()
                .filter { it.clouds <= 40 }
                .toList()
            val message = formMessage(forecastList)
            notificationSender.sendAsynchronously(subscription!!.chatId, message)
        }
    }

    private fun formMessage(forecastList: List<Forecast>): String {
        return forecastList.asSequence()
            .map { ZonedDateTime.ofInstant(Instant.ofEpochSecond(it.epochTime), ZoneId.of("+3")) }
            .groupBy { it.dayOfMonth }
            .map { (day, dates) ->
                val hours = dates.joinToString(separator = "; ") { it.hour.toString() }
                "day = $day, hours: $hours"
            }
            .joinToString(separator = "\n")
    }

    private fun createSubscription(interaction: Interaction) = Subscription(
        id = subscriptionDao.generateSubscriptionId(),
        userId = interaction.userId,
        userName = interaction.userName,
        chatId = interaction.chatId,
        chatType = interaction.chatType
    )

    companion object {
        private val log = LoggerFactory.getLogger(InteractionProcessor::class.java)
    }
}
