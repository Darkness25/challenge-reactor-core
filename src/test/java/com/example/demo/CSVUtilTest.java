package com.example.demo;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVUtilTest {

    @Test
    void converterData(){
        List<Player> list = CsvUtilFile.getPlayers();
        assert list.size() == 18207;
    }

    @Test
    void stream_filtrarJugadoresMayoresA35(){
        List<Player> list = CsvUtilFile.getPlayers();
        Map<String, List<Player>> listFilter = list.parallelStream()
                .filter(player -> player.age >= 35)
                .map(player -> {
                    player.name = player.name.toUpperCase(Locale.ROOT);
                    return player;
                })
                .flatMap(playerA -> list.parallelStream()
                        .filter(playerB -> playerA.club.equals(playerB.club))
                )
                .distinct()
                .collect(Collectors.groupingBy(Player::getClub));

        assert listFilter.size() == 322;
    }


    @Test
    void reactive_filtrarJugadoresMayoresA35(){
        List<Player> list = CsvUtilFile.getPlayers();
        Flux<Player> listFlux = Flux.fromStream(list.parallelStream()).cache();
        Mono<Map<String, Collection<Player>>> listFilter = listFlux
                .filter(player -> player.age >= 35)
                .map(player -> {
                    player.name = player.name.toUpperCase(Locale.ROOT);
                    return player;
                })
                .buffer(100)
                .flatMap(playerA -> listFlux
                         .filter(playerB -> playerA.stream()
                                 .anyMatch(a ->  a.club.equals(playerB.club)))
                )
                .distinct()
                .collectMultimap(Player::getClub);

        assert listFilter.block().size() == 322;
    }



    @Test
    void reactive_filtrarJugadoresMayoresA34porClubEspecifico(){
        List<Player> list = CsvUtilFile.getPlayers();
        Flux<Player> listFlux = Flux.fromStream(list.parallelStream()).cache();
        Mono<Map<String, Collection<Player>>> listFilter = listFlux
                .filter(player -> player.age>34)
                .filter(player -> player.club.equals("JL TEAM"))
                .distinct()
                .collectMultimap(Player::getClub);

        System.out.println(listFilter.block().size());
        listFilter.block().forEach((s, players) -> {
            System.out.println("El equipo: "+s);
            players.forEach(player -> {
                System.out.println("Jugador : "+player.name +" tiene "+ player.age+" años");
                assert player.age > 34;
            });
            assert s.equals("JL TEAM");
            assert players.size() == 4;
        });
    }

   /* @Test
    void reactive_filtrarPorNacionalidadOrdenadosPorVictorias(){
        List<Player> list = CsvUtilFile.getPlayers();
        Flux<Player> listFlux = Flux.fromStream(list.parallelStream()).cache();
        Mono<Map<String, Collection<Player>>> listFilter = listFlux
                .sort((s, player) -> player.winners)
                .distinct()
                .collectMultimap(Player::getNational);

        assert listFilter.block().size() == 164;
    }*/

    @Test
    void reactive_filterNacionalityRankingWinners() {
        List<Player> list = CsvUtilFile.getPlayers();
        Flux<Player> listFlux = Flux.fromStream(list.parallelStream()).cache();
        Mono<Map<String, Collection<Player>>> listFilter = listFlux
                .buffer(100)
                .flatMap(playerA -> listFlux
                        .filter(playerB -> playerA.stream()
                                .anyMatch(a -> a.national.equals(playerB.national)))
                ).distinct()
                .sort((k, player) -> player.winners)
                .collectMultimap(Player::getNational);

        System.out.println("Por Nacionalidad: ");
        System.out.println(listFilter.block().size());
        listFilter.block().forEach((k, players) -> {
            System.out.println("Pais: " + k);
            players.forEach(player -> {
                System.out.println(player.name + " victorias: " + player.winners);
            });
        });
    }

}
