package it.unibs.ingsw24_25.config;

import it.unibs.ingsw24_25.model.Beneficiary;
import it.unibs.ingsw24_25.model.Configurator;
import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.repository.BeneficiaryRepository;
import it.unibs.ingsw24_25.repository.ConfiguratorRepository;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ConfiguratorRepository configuratorRepository;
    private final VolunteerRepository volunteerRepository;
    private final BeneficiaryRepository beneficiaryRepository;

    public CustomUserDetailsService(ConfiguratorRepository configuratorRepository,
                                    VolunteerRepository volunteerRepository,
                                    BeneficiaryRepository beneficiaryRepository) {
        this.configuratorRepository = configuratorRepository;
        this.volunteerRepository = volunteerRepository;
        this.beneficiaryRepository = beneficiaryRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Configurator> conf = configuratorRepository.findByNickname(username);
        if (conf.isPresent()) {
            return User.builder()
                    .username(conf.get().getNickname())
                    .password(conf.get().getPassword())
                    .roles("CONFIGURATOR")
                    .build();
        }

        Optional<Volunteer> vol = volunteerRepository.findByNickname(username);
        if (vol.isPresent()) {
            return User.builder()
                    .username(vol.get().getNickname())
                    .password(vol.get().getPassword())
                    .roles("VOLUNTEER")
                    .build();
        }

        Optional<Beneficiary> ben = beneficiaryRepository.findByUsername(username);
        if (ben.isPresent()) {
            return User.builder()
                    .username(ben.get().getUsername())
                    .password(ben.get().getPassword())
                    .roles("BENEFICIARY")
                    .build();
        }

        throw new UsernameNotFoundException("Utente non trovato: " + username);
    }
}