package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.service.ConfiguratorService;

public class CommandHandler {
    private ConfiguratorService service;
    private Printer printer;
    private PromptReader prompt;

    public CommandHandler(ConfiguratorService service, Printer printer, PromptReader prompt) {
        this.service = service;
        this.printer = printer;
        this.prompt = prompt;
    }

    public void handle(String command, String[] args){}
    public void handleSetScope(String[] args){}
    public void handleSetMax(String[] args){}
    public void handleAddPlace(String[] args){}
    public void handleAddVisitType(String[] args){}
    public void handleAddVolunteer(String[] args){}
    public void handleLinkVolunteer(String[] args){}
    public void handleListPlaces(String[] args){}
    public void handleListVisitType(String[] args){}
    public void handleListVolunteer(String[] args){}
}
