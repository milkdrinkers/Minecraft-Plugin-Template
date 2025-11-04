package io.github.exampleuser.exampleplugin.messaging.broker;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.exampleuser.exampleplugin.messaging.message.BidirectionalMessage;
import io.github.exampleuser.exampleplugin.messaging.message.OutgoingMessage;
import org.jetbrains.annotations.NotNull;

public final class MessagingUtils {
    public static final class ByteUtil {
        /**
         * Get the byte array representation of an outgoing message.
         *
         * @param message the message
         * @param <T>     message type
         * @return byte array
         */
        public static <T> byte[] to(OutgoingMessage<T> message) {
            final ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(message.encode());
            return output.toByteArray();
        }

        /**
         * Get the message representation of a byte array.
         *
         * @param data byte array
         * @return the message
         */
        public static @NotNull BidirectionalMessage<?> from(byte[] data) {
            final ByteArrayDataInput input = ByteStreams.newDataInput(data);
            return BidirectionalMessage.from(input.readUTF());
        }
    }
}
