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

import de.basisdatensatz.obds.v3.*;

class ModulMapper {

    private ModulMapper() {}

    public static ModulProstataTyp map(de.basisdatensatz.obds.v2.ModulProstataTyp source) {
        if (null == source) {
            throw new IllegalArgumentException("ADT_GEKID diagnose ModulProstat should not be null at this point");
        }

        var result = new ModulProstataTyp();
        if (source.getGleasonScore() != null) {
            var gleasonScore = new GleasonScoreTyp();
            gleasonScore.setGradPrimaer(source.getGleasonScore().getGleasonGradPrimaer());
            gleasonScore.setGradSekundaer(source.getGleasonScore().getGleasonGradSekundaer());
            gleasonScore.setScoreErgebnis(source.getGleasonScore().getGleasonScoreErgebnis());
            result.setGleasonScore(gleasonScore);
            result.setAnlassGleasonScore(AnlassGleasonScoreTyp.fromValue(source.getAnlassGleasonScore()));
        }
        // oBDS v2 verwendet hier abweichend Format yyyy-MM-dd! Daher direktes Mapping
        result.setDatumStanzen(source.getDatumStanzen());
        result.setAnzahlStanzen(source.getAnzahlStanzen());
        result.setAnzahlPosStanzen(source.getAnzahlPosStanzen());
        // oBDS v2 verwendet hier abweichend Format yyyy-MM-dd! Daher direktes Mapping
        MapperUtils.mapDatumTagOderMonatGenauTypSchaetzOptional(source.getDatumPSA()).ifPresent(result::setDatumPSA);
        result.setPSA(source.getPSA());
        if (source.getCaBefallStanze() != null) {
            var caBefallStanze = new CaBefallStanzeTyp();
            caBefallStanze.setProzentzahl(source.getCaBefallStanze().getProzentzahl());
            caBefallStanze.setU(source.getCaBefallStanze().getU());
            result.setCaBefallStanze(caBefallStanze);
        }
        if(source.getKomplPostOPClavienDindo() != null) {
            result.setKomplPostOPClavienDindo(JNU.fromValue(source.getKomplPostOPClavienDindo().value()));
        }
        return result;
    }

}
