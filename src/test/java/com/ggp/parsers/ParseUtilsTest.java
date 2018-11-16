package com.ggp.parsers;

import com.ggp.IGameDescription;
import com.ggp.utils.GameRepository;
import com.ggp.utils.recall.PerfectRecallGameDescriptionWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;


import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ParseUtilsTest {

    @ParameterizedTest(name = "{1}")
    @MethodSource("correctGameDescriptions")
    void testParseGameDescription_correct(IGameDescription expected, String str) {
        assertEquals(expected, ParseUtils.parseGameDescription(str));
    }

    static Stream<Arguments> correctGameDescriptions() {
        return Stream.of(
                Arguments.of(GameRepository.leducPoker(5, 9), "LeducPoker{5,9}"),
                Arguments.of(GameRepository.leducPoker(9, 5), "LeducPoker{9,5}"),
                Arguments.of(GameRepository.iiGoofspiel(5), "IIGoofspiel{5}"),
                Arguments.of(GameRepository.rps(11), "RockPaperScissors{11}"),
                Arguments.of(GameRepository.kriegTTT(), "KriegTicTacToe"),
                Arguments.of(new PerfectRecallGameDescriptionWrapper(GameRepository.leducPoker(5, 9)), "PerfectRecall{LeducPoker{5,9}}")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "aaaa", "Leduc Poker{5,9}", "LeducPoker", "LeducPoker{5}", "LeducPoker{,}", "LeducPoker{-1, -1}"})
    void testParseGameDescription_incorrect(String str) {
        assertNull(ParseUtils.parseGameDescription(str));
    }

    @Test
    void testParseGameDescription_whitespaces() {
        assertEquals(GameRepository.leducPoker(5, 9), ParseUtils.parseGameDescription("LeducPoker \t\n{5\n\n , 9 } \t\n\r"));
    }
}