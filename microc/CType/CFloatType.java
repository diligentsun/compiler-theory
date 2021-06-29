package CType;

public class CFloatType extends CBaseType {
    private float value;


    public CFloatType(){
        this.value = 0;
    }

    public CFloatType(float value){
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
