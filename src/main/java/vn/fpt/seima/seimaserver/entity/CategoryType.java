package vn.fpt.seima.seimaserver.entity;

public enum CategoryType {
    INCOME(0),
    EXPENSE(1);


    private final int code;


    CategoryType(int code) {
        this.code = code;
    }


    public static CategoryType fromCode(int code) {
        for (CategoryType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Invalid categoryType code: " + code);
    }
}
