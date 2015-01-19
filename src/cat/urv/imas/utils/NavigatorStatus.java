/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.utils;

/**
 *
 * @author mhj
 */
public enum NavigatorStatus {
    FREE(0),
    FIRST_WINNER(1),
    IN_JOB(2);
    
    private final int value;

    private NavigatorStatus(int value) {
        this.value = value;
    }    
}
