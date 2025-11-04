package io.github.exampleuser.exampleplugin.messaging;

import com.google.gson.annotations.SerializedName;
import io.github.exampleuser.exampleplugin.event.MockEventListener;
import io.github.exampleuser.exampleplugin.event.MockEventSystem;
import io.github.exampleuser.exampleplugin.messaging.config.MessagingConfig;
import io.github.exampleuser.exampleplugin.messaging.message.BidirectionalMessage;
import io.github.exampleuser.exampleplugin.messaging.message.IncomingMessage;
import io.github.exampleuser.exampleplugin.utility.Messaging;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.exampleuser.exampleplugin.utility.Util.randomString;

/**
 * Contains all test cases.
 */
@SuppressWarnings("LoggingSimilarMessage")
@Tag("messaging")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
abstract class AbstractMessagingTest {
    private final MessengerTestParams testConfig;
    public MessagingConfig messagingConfig;
    public Logger logger = LoggerFactory.getLogger("Messaging");

    AbstractMessagingTest(MessengerTestParams testConfig) {
        this.testConfig = testConfig;
    }

    /**
     * Exposes the message broker parameters of this test.
     *
     * @return the message broker test config
     */
    public MessengerTestParams getTestConfig() {
        return testConfig;
    }

    @BeforeEach
    void beforeEachTest() {
    }

    @AfterEach
    void afterEachTest() {
        MockEventSystem.clear();
    }

    @AfterAll
    void afterAllTests() {
        Messaging.getHandler().doShutdown(); // Shut down the message broker after all tests have been run
    }

    private record TestMessage(@SerializedName("data") String data) {
        @Override
        public String data() {
            return data;
        }
    }

    @Test
    @Order(1)
    @DisplayName("Sending")
    void testSending(TestInfo testInfo) throws InterruptedException, ExecutionException {
        final BidirectionalMessage<Object> message = BidirectionalMessage.builder()
            .channelId("message")
            .payload(randomString())
            .build();

        final boolean messageSent = Messaging.send(message).get();
        Assertions.assertTrue(messageSent, "Message should have been sent but wasn't");
    }

    @RepeatedTest(25)
    @Order(2)
    @DisplayName("Receiving")
    void testReceiving(TestInfo testInfo, RepetitionInfo repetitionInfo) throws InterruptedException, ExecutionException {
        logger.info("Starting test iteration for test: {} #{}", testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());

        final BidirectionalMessage<Object> message = BidirectionalMessage.builder()
            .channelId("message")
            .payload(randomString())
            .build();

        logger.info("Created message with UUID: {} for test: {} #{}", message.getUUID(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());

        final CountDownLatch receiveLatch = new CountDownLatch(1);
        final AtomicReference<IncomingMessage<?, ?>> receivedMessage = new AtomicReference<>();

        final MockEventListener listener = (event) -> {
            if (event instanceof MockSyncMessageEvent incomingMessage) {
                logger.info("Listener received message UUID: {} for test: {} #{}", incomingMessage.getMessage().getUUID(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());
                if (message.getUUID().equals(incomingMessage.getMessage().getUUID())) {
                    receivedMessage.set(incomingMessage.getMessage());
                    receiveLatch.countDown();
                }
            }
        };

        logger.info("Registering listener for test: {} #{}", testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());
        MockEventSystem.registerListener(listener);

        final boolean messageSent = Messaging.send(message).get();
        logger.info("Message sent status: {} for message UUID: {} in test: {} #{}", messageSent, message.getUUID(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());
        Assertions.assertTrue(messageSent, "Message should have been sent but wasn't");

        final boolean messageReceived = receiveLatch.await(10, TimeUnit.SECONDS);
        logger.info("Message received status: {} for message UUID: {} in test: {} #{}", messageReceived, message.getUUID(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());
        Assertions.assertTrue(messageReceived, "Message should have been received within timeout");
        Assertions.assertNotNull(receivedMessage.get(), "Received message should not be null");
        Assertions.assertEquals(message.getUUID(), receivedMessage.get().getUUID(), "Received message UUIDs should match");
        logger.info("Message received UUID: {}, channelId: {} in test: {} #{}", receivedMessage.get().getUUID(), receivedMessage.get().getChannelID(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());

        logger.info("Test iteration completed successfully for test: {} #{}", testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());
    }

    @RepeatedTest(25)
    @Order(3)
    @DisplayName("Message Integrity")
    void testMessageIntegrity(TestInfo testInfo, RepetitionInfo repetitionInfo) throws InterruptedException, ExecutionException {
        logger.info("Starting test iteration for test: {} #{}", testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());

        final TestMessage payload = new TestMessage(randomString());
        final BidirectionalMessage<Object> message = BidirectionalMessage.builder()
            .channelId("message")
            .payload(payload)
            .build();

        logger.info("Created message with UUID: {} for test: {} #{}", message.getUUID(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());

        final CountDownLatch receiveLatch = new CountDownLatch(1);
        final AtomicReference<IncomingMessage<?, ?>> receivedMessage = new AtomicReference<>();

        final MockEventListener listener = (event) -> {
            if (event instanceof MockSyncMessageEvent incomingMessage) {
                logger.info("Listener received message UUID: {} for test: {} #{}", incomingMessage.getMessage().getUUID(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());
                if (message.getUUID().equals(incomingMessage.getMessage().getUUID())) {
                    receivedMessage.set(incomingMessage.getMessage());
                    receiveLatch.countDown();
                }
            }
        };

        logger.info("Registering listener for test: {} #{}", testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());
        MockEventSystem.registerListener(listener);

        final boolean messageSent = Messaging.send(message).get();
        logger.info("Message sent status: {} for message UUID: {} in test: {} #{}", messageSent, message.getUUID(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());
        Assertions.assertTrue(messageSent, "Message should have been sent but wasn't");

        final boolean messageReceived = receiveLatch.await(10, TimeUnit.SECONDS);
        logger.info("Message received status: {} for message UUID: {} in test: {} #{}", messageReceived, message.getUUID(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());
        Assertions.assertTrue(messageReceived, "Message should have been received within timeout");
        Assertions.assertNotNull(receivedMessage.get(), "Received message should not be null");
        Assertions.assertEquals(message.getUUID(), receivedMessage.get().getUUID(), "Received message UUIDs should match");
        Assertions.assertEquals(message.getChannelID(), receivedMessage.get().getChannelID(), "Received message channel id should match");
        Assertions.assertNotNull(receivedMessage.get().getPayload(), "Received message payload should not be null");
        Assertions.assertEquals(message.getPayloadType().getName(), receivedMessage.get().getPayloadType().getName(), "Received message payload name should match");
        Assertions.assertEquals(payload, receivedMessage.get().getPayload(), "Received message payload should match");
        logger.info("Message received UUID: {}, channelId: {} in test: {} #{}", receivedMessage.get().getUUID(), receivedMessage.get().getChannelID(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());

        Assertions.assertDoesNotThrow(() -> (TestMessage) receivedMessage.get().getPayload(), "Received message payload should not throw on cast");
        final TestMessage payloadReceived = (TestMessage) receivedMessage.get().getPayload();
        Assertions.assertNotNull(payloadReceived.data(), "Received message payload data should not be null");
        Assertions.assertEquals(payload.data(), payloadReceived.data(), "Received message payload data should match");
        logger.info("Message received payload: {} in test: {} #{}", payloadReceived.data(), testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());

        logger.info("Test iteration completed successfully for test: {} #{}", testInfo.getDisplayName(), repetitionInfo.getCurrentRepetition());
    }
}
