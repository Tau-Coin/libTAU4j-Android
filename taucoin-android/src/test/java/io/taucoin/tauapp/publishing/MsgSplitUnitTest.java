package io.taucoin.tauapp.publishing;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class MsgSplitUnitTest {
    private static final int BYTE_LIMIT = 900;

    @Test
    public void TestMsgSplit() {
        String msg = "中新网上海1月3日电(周卓傲)1月2日晚，特斯拉宣布2020年共生产和交付近50万辆电动车，基本达成计划的交付目标。此外，Model Y的量产已于上海超级工厂启动，预计短期即可交付。\n" +
                "　　从特斯拉公布的全年生产以及交付的数据报告中可以看到，特斯拉在2020年累计生产509737辆电动车，交付499550辆电动车。特斯拉方面表示，此次公布的交付数量统计稍微偏向保守，最终的交付数字可能会有0.5%或更多的差异。如果最终的交付数字较目前交付数据多出0.5%，那么意味着特斯拉将完成全年交付目标。\n" +
                "　　乘用车市场信息联席会数据显示，2020年前11月，特斯拉Model 3在中国市场的销量达到11.36万辆。值得一提的是，2021年1月1日特斯拉中国宣布，中国制造中型SUV Model Y 正式发售，这一消息宣布后，特斯拉官网的订单页面一度瘫痪。对此，官方回应称：“官网订单页面由于访问量激增，可能暂时无法刷新，请大家耐心稍等片刻。”\n" +
                "　　汽车行业分析师费哲逸指出，中国市场在特斯拉全球份额中扮演非常重要的角色，乘联会预测称，到2021年中国电动车总销量将达到170万辆，可以看到中国电动车市场依然处于高速增长的阶段，“中国消费者非常喜爱SUV车型，中国制造Model Y的上市将会进一步提升特斯拉销量。”(完)";
        long startTime = System.currentTimeMillis();
        List<byte[]> list = TestSplitText(msg);
        long endTime = System.currentTimeMillis();
        System.out.println("times:: " + (endTime - startTime));
        for (byte[] bytes : list) {
            System.out.println("bytes size:: " + bytes.length +
                    " fragmentContent::" + new String(bytes, StandardCharsets.UTF_8));
        }
    }

    private List<byte[]> TestSplitText(String msg){
        List<byte[]> list = new ArrayList<>();
        int msgSize = msg.length();
        int statPos = 0;
        int endPos = 1;
        byte[] lastFragmentBytes = null;
        // 拆分消息数据
        do {
            if (endPos >= msgSize) {
                endPos = msgSize;
            }
            String fragment = msg.substring(statPos, endPos);
            byte[] fragmentBytes = fragment.getBytes(StandardCharsets.UTF_8);
            if (fragmentBytes.length > BYTE_LIMIT) {
                statPos = endPos - 1;
                if (lastFragmentBytes != null) {
                    list.add(lastFragmentBytes);
                }
            } else if (fragmentBytes.length == BYTE_LIMIT || endPos == msgSize) {
                statPos = endPos;
                list.add(fragmentBytes);
            } else {
                endPos += 1;
            }
            lastFragmentBytes = fragmentBytes;
        } while (statPos < msgSize);
        return list;
    }
}