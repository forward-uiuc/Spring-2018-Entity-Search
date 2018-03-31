/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.forward.entitysearch.entitylayoutanalysis;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.StringTokenizer;


/**
 * 
 *
 * @author longpt214
 */
public class EntityLayoutTokenizer extends Tokenizer {

	private static final String UNIQUE_ENTITY_TOKEN = "oentityo";

	private Iterator<Integer> entityPositions;

    private int offset = 0;
    private int curPos = 0;


    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

    public EntityLayoutTokenizer() {
        this(true, false);
    }
    
    public EntityLayoutTokenizer(boolean p1, boolean p2) {
        super();
    }

    private void tokenize(Reader input) throws IOException {
        int numChars;
        char[] buffer = new char[1024];
        StringBuilder stringBuilder = new StringBuilder();
        try {
            while ((numChars =
                input.read(buffer, 0, buffer.length)) != -1) {
                stringBuilder.append(buffer, 0, numChars);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        ArrayList<Integer> entityPos = new ArrayList<>();
        StringTokenizer tokens = new StringTokenizer(stringBuilder.toString());
        while (tokens.hasMoreTokens()) {
            final String token = tokens.nextToken();
            if (accept(token)) {
            	    String[] tmp = token.split("\\Q|\\E");
            	    int pos = Integer.parseInt(tmp[1].split("\\Q+\\E")[0]);
            	    entityPos.add(pos);
            }
            Collections.sort(entityPos);
            this.entityPositions = entityPos.iterator();
        }
    }

    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        if (this.entityPositions.hasNext()) {
        		Integer pos = this.entityPositions.next();
        		posIncrAtt.setPositionIncrement(pos-curPos);              
            termAtt.append(UNIQUE_ENTITY_TOKEN);
            offsetAtt.setOffset(pos,pos);
            curPos = pos;  
            return true;
        } 
        return false;
    }

    /**
     * Only accept the word characters.
     */
    private final boolean accept(String token) {
        return token.contains(UNIQUE_ENTITY_TOKEN);
    }

    @Override
    public final void end() throws IOException {
        super.end();
        final int finalOffset = correctOffset(offset);
        offsetAtt.setOffset(finalOffset, finalOffset);
        posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement());
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        offset = 0;
        curPos = -1;
        tokenize(input);
    }
}