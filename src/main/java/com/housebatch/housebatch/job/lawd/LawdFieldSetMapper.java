package com.housebatch.housebatch.job.lawd;

import com.housebatch.housebatch.core.entity.Lawd;
import lombok.Getter;
import lombok.Setter;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

// FieldSetMapper Interface를 구현하면, FieldSet객체를 T타입으로 변환하여 사용한다.
@Getter
@Setter
public class LawdFieldSetMapper implements FieldSetMapper<Lawd> {

    public static final String LAWD_CD = "lawdCd";
    public static final String LAWD_DONG = "lawdDong";
    public static final String EXIST = "exist";

    private static final String EXIST_NAME = "존재";

    @Override
    public Lawd mapFieldSet(FieldSet fieldSet) throws BindException {
        Lawd lawd = new Lawd();
        lawd.setLawdCd(fieldSet.readString(LAWD_CD));
        lawd.setLawdDong(fieldSet.readString(LAWD_DONG));
        lawd.setExist(fieldSet.readBoolean(EXIST, EXIST_NAME));
        return lawd;
    }
}
