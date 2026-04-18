package cn.lysoy.jingu3.skill.tool;

import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * 四则运算表达式求值；仅允许数字与 + - * / . ( ) 及空白，防注入。
 */
@Component
public class CalculatorTool implements Jingu3Tool {

    private static final Pattern SAFE = Pattern.compile("^[0-9+\\-*/().\\s]+$");

    @Override
    public String id() {
        return "calculator";
    }

    @Override
    public String description() {
        return "对仅含数字与运算符的表达式求值；input 为表达式字符串，例如 1+2*3 或 (10-3)/2。";
    }

    @Override
    public String execute(String input) throws ToolExecutionException {
        if (input == null || input.isBlank()) {
            throw new ToolExecutionException("calculator 需要非空表达式");
        }
        String expr = input.trim();
        if (!SAFE.matcher(expr).matches()) {
            throw new ToolExecutionException("表达式含非法字符（仅允许 0-9 + - * / . ( ) 与空格）");
        }
        try {
            double v = new ExpressionBuilder(expr).build().evaluate();
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new ToolExecutionException("计算结果非有限数");
            }
            return format(v);
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException("表达式无法求值: " + e.getMessage(), e);
        }
    }

    private static String format(double v) {
        BigDecimal bd = BigDecimal.valueOf(v).stripTrailingZeros();
        if (bd.scale() < 0) {
            bd = bd.setScale(0, RoundingMode.UNNECESSARY);
        }
        return bd.toPlainString();
    }
}
