package il.cshaifasweng.OCSFMediatorExample.entities;

import java.util.ArrayList;

public class ResturantChain {
    int ChainID;
    String Name;
    ArrayList<Branch> Branches;

    public ResturantChain(int chainID, String name, ArrayList<Branch> branches) {
        ChainID = chainID;
        Name = name;
        Branches = branches;
    }

    public int getChainID() {
        return ChainID;
    }

    public void setChainID(int chainID) {
        ChainID = chainID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public ArrayList<Branch> getBranches() {
        return Branches;
    }

    public void setBranches(ArrayList<Branch> branches) {
        Branches = branches;
    }
}
