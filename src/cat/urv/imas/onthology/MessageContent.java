/**
 *  IMAS base code for the practical work.
 *  Copyright (C) 2014 DEIM - URV
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cat.urv.imas.onthology;

import java.io.Serializable;

/**
 * Content messages for inter-agent communication.
 */
public class MessageContent implements Serializable {

    /**
     * Message sent from Coordinator agent to Central agent to get the whole
     * city information.
     */
    public static final String GET_MAP = "Get map";
    public static final String NEW_STEP = "New step";
    
    private String messageType;
    private Object content;

    public MessageContent() {
    }

    public MessageContent(String messageType, Object content) {
        this.messageType = messageType;
        this.content = content;
    }

    public Object getContent() {
        return content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    

}
