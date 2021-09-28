package com.api.communications;

public class MultiHopHighLevelCommLink {

    private final int numUAV;
    private final HighlevelCommLink commLink;

    public MultiHopHighLevelCommLink(int numUAV){
        this.numUAV = numUAV;
        this.commLink = new HighlevelCommLink(numUAV);
    }


}
