package CType;

public class CCharType extends CBaseType {
    private char value;

    CCharType(){
        value = 0;
    }

    public CCharType(char c){
        value = c;
    }

    public char getValue() {
        return value;
    }

    public void setValue(char value) {
        this.value = value;
    }
}
