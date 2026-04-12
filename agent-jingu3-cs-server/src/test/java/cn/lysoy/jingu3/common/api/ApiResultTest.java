package cn.lysoy.jingu3.common.api;

import cn.lysoy.jingu3.common.enums.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResultTest {

    @Test
    void ok_wrapsData() {
        ApiResult<String> r = ApiResult.ok("x");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getCode()).isEqualTo("0");
        assertThat(r.getData()).isEqualTo("x");
    }

    @Test
    void fail_usesErrorCode() {
        ApiResult<Void> r = ApiResult.fail(ErrorCode.BAD_REQUEST, "bad");
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(r.getMessage()).isEqualTo("bad");
    }
}
