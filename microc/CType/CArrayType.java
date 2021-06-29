package CType;

public class CArrayType extends CBaseType{
    private CBaseType[] value;
    private int length;

    public CArrayType(){
        value = null;
    }

    public CArrayType(CBaseType[] array){
        value = array;
    }

    public CBaseType[] getValue() {
        return value;
    
    }

    public void setValue(CBaseType[] value) {
        this.value = value;
    }
}
