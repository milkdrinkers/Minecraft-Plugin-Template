package io.github.exampleuser.example;

import io.github.exampleuser.example.api.ExampleAPI;

class ExampleAPIProvider extends ExampleAPI implements Reloadable {
    private final Example plugin;

    ExampleAPIProvider(Example plugin) {
        super();
        this.plugin = plugin;
        setInstance(this);
    }
}
