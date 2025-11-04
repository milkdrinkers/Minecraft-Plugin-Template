package io.github.exampleuser.exampleplugin.messaging;

@SuppressWarnings("unused")
public record MessengerTestParams(String type) {
    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private String type;

        private Builder() {
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public MessengerTestParams build() {
            return new MessengerTestParams(type);
        }
    }
}
