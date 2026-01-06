package MyApp.misc;


//======================================================================
// Msg
public record Msg(String sender, int type, String details) {
    //------------------------------------------------------------
    // toString
    @Override
    public String toString() {
        return sender + "(" + type + ") -- " + details;
    } // toString
} // Msg
