package it.unibs.ingsw24_25.service;

import java.util.Objects;

public class FirstAccessService {
    private static final String INVALID_NUMBER_MESSAGE = "Inserire un numero intero valido";

    public interface ConfiguratorFirstAccessView {
        void showSeparator();

        void showIntro();

        void showCompletion();

        String readDefaultNickname();

        String readDefaultPassword();

        void showDefaultCredentialsSuccess();

        String readPersonalNickname();

        String readPersonalPassword();

        void showPersonalCredentialsSuccess();

        String readTerritorialScope();

        String readMaxPeople();

        void showError(String message);
    }

    public interface VolunteerFirstAccessView {
        void showSeparator();

        void showVolunteerHeader();

        String readVolunteerDefaultPassword();

        void showOperationAborted();

        String readVolunteerNewNickname();

        String readVolunteerNewPassword();

        String readVolunteerConfirmPassword();

        void showPasswordMismatch();

        void showVolunteerSuccess(String newNickname);

        void showError(String message);
    }

    private final ConfiguratorService configuratorService;
    private final VolunteerService volunteerService;

    public FirstAccessService(ConfiguratorService configuratorService, VolunteerService volunteerService) {
        this.configuratorService = Objects.requireNonNull(configuratorService, "ConfiguratorService non può essere nullo");
        this.volunteerService = Objects.requireNonNull(volunteerService, "VolunteerService non può essere nullo");
    }

    public void runConfiguratorFirstAccess(ConfiguratorFirstAccessView view) {
        Objects.requireNonNull(view, "View non può essere nulla");
        if (!configuratorService.hasPendingConfiguratorSeeds()) {
            return;
        }

        view.showSeparator();
        view.showIntro();
        view.showSeparator();

        String defaultNickname = verifyDefaultCredentials(view);
        setPersonalCredentials(view, defaultNickname);
        configureTerritorialScope(view);
        configureMaxParticipants(view);

        view.showSeparator();
        view.showCompletion();
        view.showSeparator();
    }

    public String runVolunteerFirstAccess(String nickname, VolunteerFirstAccessView view) {
        Objects.requireNonNull(nickname, "Il nickname non può essere nullo");
        Objects.requireNonNull(view, "View non può essere nulla");
        String currentNickname = nickname.trim();
        if (currentNickname.isEmpty()) {
            throw new IllegalArgumentException("Il nickname non può essere vuoto");
        }

        view.showSeparator();
        view.showVolunteerHeader();
        view.showSeparator();

        while (true) {
            String defaultPassword = view.readVolunteerDefaultPassword();
            if (defaultPassword == null) {
                view.showOperationAborted();
                return null;
            }
            try {
                volunteerService.verifyDefaultCredentials(currentNickname, defaultPassword);
                break;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                view.showError(safeMessage(ex));
            }
        }

        while (true) {
            String newNickname = view.readVolunteerNewNickname();
            if (newNickname == null) {
                return null;
            }

            String newPassword = view.readVolunteerNewPassword();
            if (newPassword == null) {
                return null;
            }

            String confirmation = view.readVolunteerConfirmPassword();
            if (confirmation == null) {
                return null;
            }

            if (!Objects.equals(newPassword, confirmation)) {
                view.showPasswordMismatch();
                continue;
            }

            try {
                volunteerService.setPersonalCredentials(currentNickname, newNickname, newPassword);
                view.showVolunteerSuccess(newNickname);
                return newNickname;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                view.showError(safeMessage(ex));
            }

        }
    }

    private String verifyDefaultCredentials(ConfiguratorFirstAccessView view) {
        boolean verified = false;
        String sanitizedNickname = null;
        while (!verified) {
            String nickname = view.readDefaultNickname();
            String password = view.readDefaultPassword();
            try {
                configuratorService.verifyDefaultCredentials(nickname, password);
                view.showDefaultCredentialsSuccess();
                verified = true;
                sanitizedNickname = nickname == null ? null : nickname.trim();
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(safeMessage(e));
            }
        }
        return sanitizedNickname;
    }

    private void setPersonalCredentials(ConfiguratorFirstAccessView view, String defaultNickname) {
        boolean stored = false;
        while (!stored) {
            String nickname = view.readPersonalNickname();
            String password = view.readPersonalPassword();
            try {
                configuratorService.setPersonalCredentials(defaultNickname, nickname, password);
                view.showPersonalCredentialsSuccess();
                stored = true;
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(safeMessage(e));
            }
        }
    }

    private void configureTerritorialScope(ConfiguratorFirstAccessView view) {
        boolean configured = false;
        while (!configured) {
            String scope = view.readTerritorialScope();
            try {
                configuratorService.setTerritorialScope(scope);
                configured = true;
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(safeMessage(e));
            }
        }
    }

    private void configureMaxParticipants(ConfiguratorFirstAccessView view) {
        boolean configured = false;
        while (!configured) {
            String rawValue = view.readMaxPeople();
            try {
                int value = Integer.parseInt(rawValue);
                configuratorService.setMaxPeoplePerSubscription(value);
                configured = true;
            } catch (NumberFormatException e) {
                view.showError(INVALID_NUMBER_MESSAGE);
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(safeMessage(e));
            }
        }
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null ? "" : message;
    }
}
