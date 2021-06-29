package CType;

public class CIntType extends CBaseType {
    private int value;

    public CIntType(){
        value = 0;
    }

    public CIntType(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }


}
