package cn.lysoy.jingu3.common.api;

import cn.lysoy.jingu3.common.enums.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResultTest {

    @Test
    void ok_wrapsData() {
        ApiResult<String> r = ApiResult.ok("x");
        assertThat(r.success()).isTrue();
        assertThat(r.code()).isEqualTo("0");
        assertThat(r.data()).isEqualTo("x");
    }

    @Test
    void fail_usesErrorCode() {
        ApiResult<Void> r = ApiResult.fail(ErrorCode.BAD_REQUEST, "bad");
        assertThat(r.success()).isFalse();
        assertThat(r.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(r.message()).isEqualTo("bad");
    }
}
