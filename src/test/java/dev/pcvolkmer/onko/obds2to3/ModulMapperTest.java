/*
 * This file is part of obds2-to-obds3
 *
 * Copyright (c) 2024 the original author or authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.pcvolkmer.onko.obds2to3;

import de.basisdatensatz.obds.v2.JNUTyp;
import de.basisdatensatz.obds.v2.ModulProstataTyp;
import de.basisdatensatz.obds.v3.AnlassGleasonScoreTyp;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;

class ModulMapperTest {

    @Test
    void testShouldMapModuleProstata() throws DatatypeConfigurationException {
        var source = new ModulProstataTyp();
        source.setAnlassGleasonScore("S");
        source.setAnzahlStanzen(2);
        source.setAnzahlPosStanzen(3);

        var calendarPSA = new GregorianCalendar();
        calendarPSA.clear();
        calendarPSA.set(2024, 9, 21);
        source.setDatumPSA(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendarPSA));

        var calendarStanzen = new GregorianCalendar();
        calendarStanzen.clear();
        calendarStanzen.set(2024, 9, 22);
        source.setDatumStanzen(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendarStanzen));

        var gleasonScore = new ModulProstataTyp.GleasonScore();
        gleasonScore.setGleasonGradPrimaer("4");
        gleasonScore.setGleasonGradSekundaer("5");
        gleasonScore.setGleasonScoreErgebnis("9");
        source.setGleasonScore(gleasonScore);

        source.setKomplPostOPClavienDindo(JNUTyp.J);

        var caBefallStanze = new ModulProstataTyp.CaBefallStanze();
        caBefallStanze.setProzentzahl(50);
        source.setCaBefallStanze(caBefallStanze);

        var actual = ModulMapper.map(source);

        assertThat(actual.getAnlassGleasonScore()).isEqualTo(AnlassGleasonScoreTyp.S);
        assertThat(actual.getAnzahlStanzen()).isEqualTo(2);
        assertThat(actual.getAnzahlPosStanzen()).isEqualTo(3);
        assertThat(actual.getDatumPSA().toXMLFormat()).startsWith("2024-10-21");
        assertThat(actual.getDatumStanzen().toXMLFormat()).startsWith("2024-10-22");
        assertThat(actual.getGleasonScore().getGradPrimaer()).isEqualTo("4");
        assertThat(actual.getGleasonScore().getGradSekundaer()).isEqualTo("5");
        assertThat(actual.getGleasonScore().getScoreErgebnis()).isEqualTo("9");
        assertThat(actual.getKomplPostOPClavienDindo()).isEqualTo(de.basisdatensatz.obds.v3.JNUTyp.J);
        assertThat(actual.getCaBefallStanze().getProzentzahl()).isEqualTo(50);
    }

}
