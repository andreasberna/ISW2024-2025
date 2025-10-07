package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.service.ConfiguratorService;

import java.util.Map;

public class CommandRouter {
    private ConfiguratorService service;
    private Map<String, CommandHandler> handlers;
    private Printer printer;
    private PromptReader prompt;

    public CommandRouter(ConfiguratorService service, Printer printer, PromptReader prompt) {
        this.service = service;
        this.printer = printer;
        this.prompt = prompt;
    }

    public void register(String command, CommandHandler handler) {}
    public void dispatch(String line){}
    public String[] splitArgs(String line){
        return line.split(" ");
    }

}
