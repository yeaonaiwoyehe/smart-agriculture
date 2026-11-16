package com.agri.cms.web.controller.key;

import com.agri.cms.common.utils.AESUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionFlowTest {
    private static final Logger logger = LoggerFactory.getLogger(EncryptionFlowTest.class);
    private static final String AES_KEY = "3tfcvjS4bNechblUsBkSv9EJ73+oIm/jCC27M/azlUo=";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_SOURCE = "web";

    public static void main(String[] args) {
        try {
            // 测试各种请求场景
//            testGetRequest();
//            System.out.println("\n" + "=".repeat(80) + "\n");
//
//            testPostRequest();
//            System.out.println("\n" + "=".repeat(80) + "\n");

            String content = "paFMUVlvTEE4E3z3rIAn8ocTsHI3mnFcs4FU5YuirOuDvqMobvivBmBe2Ee4thtRvvSUHswnVBCHreshx2PqGMSdR7r6D1ZuVC9xwEoiKuRXpSwTMX0gqJDHYCOj4sOqUPDz8CP8V3ldkIvS-uCEUwb0ztpHJZMhqZr2gLJbM5QthFlsAgpqw9SelopguoTxvjFEy_bJ_6JejiMFgwTxzVHBnfbQ1gEEpgz8iuNu1mIg65Ca-R96gRD7RWrnA45J2eTjfD1kaMZIQRQ0N4a18KnzQ5FJ9OtheXNFQaC20dQJ5Z4je00GmIIuM4Wp_ASKTuDwIDV1Noj_ihg-PCnYwzmx6hBsMu5Mdke6JGzt9yk0FG3knmEImkDfASD6ywrMv_xOdCSh0IktscLuMm4EjugMy7XAcmJHuP4t_lcZk7oUz-4QnB8CZ-LLOjEVA5CokdHDgs0aRhDFZ_pdalLfCDJXWLHTKIP2RoIkqsVNuqdUIurKB8gMavzxEQjdsdoY0AVyD9THOA6udtKBhT6EIf1fT5qh0Z1C1PSdNQIfv2CD895WOl7VpiMvKodXilSvmNRfvQhkbdBbgxy4Vs0DtwGcAhJwCCSYfIDOumVs6b8w-YuvsgyIdIInEaOzQyjUksnoBfs8--dMj_NZxBfGQwd7QG-52thQCkJr7WySrmSIMEWgi6qExnNPfskABpWc1gngLKl3R1Ry2Mo1x95TkbBCz2mkzXRwVv-B1ykOHg3oXGIVJiY0Iz_3RzIq9-KxW6D4X8gDsWZ6lSVFF8D_EcIn3-tKAIyHZO9xB4gW0SfjJh4_QSjquBqkrAwspeZFQV4oPVRhPYUF6kG_Eq9f7_tzaRPbbPTSBqFiZFq0f1pP0vZzMqvSUe5xC0n6cLhf6OTx1LsxtEK1TWJt4kdq3XB09coRxKI9PUuO8DJrTAHNORjOHiPsHFPye0eVpiE0MZyNEyDHQgk7AanFDAJD7BTaY-KHgptDfn9BR4rrde-sdyxLHoPjHynGRnYJE8XfeGpU1dEzUII0NskEAey6589HZeM0q_ws3FSwPVfr0-UWKTnytCE0Mpz5mj-HJic-HZdiNqiFaqnmXdG1chfeKGRi09eEJFQ21pG1mRk97z7NR9S_fRhxzg_s3fL9KGhV-9eUOE_QnmxT17I3fN0AWfaIMzJRkBjf34Rz7zBE1xy1Hs1nZSzbtctNbjGHPy71XxUpeRyMKlKVpC1JgCMHUN1IatwE0RxbTZGu9b1x5e5V8kE7viSMc5cyJ6PCnzLpg7wQkBYQtY9rlxKqXX6B5eMnC_cs1xHWTmDNx5d3WENGcF6dLTY6xvXzlmHH3-_gn8-TIL5-y8h3Vy1pF6qUka6JqWTmAWcqNHFCzV5MgvH8XIErM4nOyo1ym3mzlJn2UBa1ChJ8sF6QBIH3QD6P-8KuOeL9nHzoKtr1GD3-KhsBjm5Zxovuizpv_7VwryTT9HO6ZYGtvwgdmN2qSwdf4fId2rd_c5EV7HCv_edkslcNNqQ6smvoYw_KNNhG7POCzgOvPT5VmNwG7FQ4PqSxSOOwcPA2-BMMVSIT4qorsU4GZbeX77oUe6WmfJij3v1lcy25_TINWdqZAAyG6ADjSpBy_xm1wFKJt_TKCRpKxB5xo-6ltsRy6yn2aXvfardV0wA_DTKtHrNvsGXgxG9tAjK5zaWITaCeOM1a2kwnBsaTZS0pKI84j15zTjXaWrOeFIazPLVqN7YNjFOh0VymLbj4YBNnIpKbkxu6Weudguaz8rUQklW_Wx0WNjyYk3RfPii4I79BV3pHk6w_r_A0xH-oEF1k2SvuB-AGak5WF0lNHiotERQK18WgUqjV_yVDMHHaY6NP7JVjTFoO4Le38BAKLPFa8iE--WjiDeSS7KwcFCFy9iAv1YmhTHatgD-OwvbDIoMN1FEp2PjkRvx9WfznGlHGCBM7g6qE1EstiuATAPCtZTgA-7zvL1HJql5qH3O5pkjjQD41-4ZVO5f1H1PAO4yF6xKIqhgJqoTOizYXZSsW1T1y_wx_6A8GQmUavRB3LHc8HkTYp9NtqoAmS__FE9nhffdub06_7YEWkBehtALZxI1iwh4lwyUbi30mtUT2x6cKnco-Q12Aa5Rleeu7nAGLOiei_sSpJYZNfYRj6Q2iwQaYM8X5PkY5mvxGJEIRHqwQOAqk39BSJLtODzWVKYXyYwuwMgL1Hjxf5eoRm_6wO3vtfWqcG49SSBHjb6EBqnxIG9mZIM6T35uVcDR_a73brWzjn9vbKBFyjoGSejYlijrXS_um5dJeU5Oca6TD2x_rEwdR-1LyCzTVVFmlSCgIltHhUo8ra7n0t47HpuaSv6HSJWGd9A0E_nvLpmKc7lTH1P1tgtD7gtGisf2Y6yQsRttggPkpRipiUDaxlYz2pdKfY0FV73hljIJpSlCrxtCFTgqu8h5HcjjaPEbphQMbo6lfAyoLI5lcU1LeWqckC9aIZeCyjuGP2K-xCW7fzlRFIxz82QDUXoW7_uox5-mtynkHD3DcCk_jYq7WDChBtGplYe1bRkneJ-4WHqfnWc1E2sFdVbSZLnbgxBawPsh6C_b-UVUHHXbM8TZLJRogqEqnkWxJwJJSxmfsYM1GeUySydPzrKzSTZ4X2agI_BbwLYcGRQv1whDNK7_DJk2o8JVd6cK-SET9kZQLNRZyDJsL1UA7PIXU-9_PYMnHqrf4eP7HwVKY2ZPTFL70lWzB-V2ijOPaKLfMBNOumONqh8RxuMV-Bs58uEbqkypE5La-VpLW22aOxPm0WXQ_6an0p4xioj5doWi4Lf0_C5o92vbeyvauQWxqMsmVk36ZPEcoz0hB-htKwC3bUcoViLJYCLyGZ3MJWyKcuB5luDbkwwoTQOH3zvJ9ZQ_wNbyuUyI5lkkP6iscLN84NwKKOZghK1NzKAzACUctuyGlCN6oo9BF8OK2a4VQKBxUYBaW0HcNk1lsudU8wwYubR7EIEhk_aPB8X4cOLepyvwxZWTRaxwHmKFdDkPuDnkzl2i86ji3IjvWbyqTyf5u3hXA7neKIKPdb_oOl3fnoLCZLrgz36aLPs91ppH9nIm3J3qkVtXFava48xivlWvqpMTAcp20QZj-z4lWaY1D4gbDigMt6dsHTbxXxhFT8CXkZHvNB8HVK1B1U8Bfr3RaIb1h8-ubOS30esVzTWiM6z7ymHEa1KVIPb9W1HgvUi5mejmeKtPSVMfzBQ9-4wCwQpYmYdCLRQ46S1sFB-FJ5Rq8gDGAN7nJo0kU0tt3PwDDxr9Ts6Ye3en_hIPXe-SAsdaw_esj_YRTdgp_0-w6zINhiGAqJ7JTgqsU5KFTkEnkRXKXYsy1PvCXnXj2yd5yumFoyfZt8Z1jOHpRcH4__S4oyQvQogawSu7tqUQctiAU3-Qk7HaIMliqAjAlXPAW0BFlN15T49Y0uJjqMPioOkYAtojNjGDcLcNHw7niJe7V0wDzKzIWmSN-BRdor2hZdtfUkvFGGAw6T7Ra_3esmA37fbtUAZN4gY-fJ3X6ILQsvmQYARa7XZ-GhpWeoC8c3BYCX-pLGE2cN9bw47xqNlaNMI_az-7HvnkHJbQr-a-gzFNU-bYjYLI1lChPb-UwRLP-r6fxToG-uwXWP5GuuKWgqTEw_JtYHyusbur1acrRjLmLiMyJx9eTDQC24anb-nx9ctmUmbWd8ZwltdOn867jQ4LEybZ3z2bePgCDQukr6qkAK9S6Iwn5JmOh7NvL9fszd_zTecQcvzTh62qYrBsGxecvJ26cmm_Re9KU0kephNkcPXU9KYT6uvsWVwQn0jdkL-m7z26dYG8c6RZ5-FX2o_p3SKXwckJ2jc-Bt5s_Tqdd_4thCCdaKMnqZuZqkzyHvFbytpOaeTfhQ-0PW2DzA7kbqYY01eWIOWGDStz6wG0Srr4xE0jqFjUd9zTDU1DGsaQcCIHMEBxhdOmk9lTktbKkUG_lm95rEhigcZ_da9X4ue9IM37bYRDwmrg6JP5OIlY8SpEOL061VyyoHbZgY44VTx5Xzc2fvVtaW01ysZHCsatGc8afZUM7teQE8bKclMMWg4g1l7mSUV-dJhpo5BuLyoI7gSTA-mSWTHyfy6fK_es1kHN_NAB9ZkHRIkq1hbCC7l48ZLQA-L9v2tDcb0W8TUR_vUxMFwzAnKTuNz1zUdPH5wTGMm1TzO4H4bbTBXDtHfY7moxcvKdW-puIh-JPtyqGdX-2bhPmHKtk7_8NBmDbP56WF4Xhj4EoJS0JHrcX0p6LAavWrS8l_k3CN-PABGosICkNSHbxQpoY3kYGDT2Ro5j0ahVFlR5qy_HiN7vrXfquTWOPw5FRN1mAIvbBk3CUWS_rXMsaBTXGTzUM5Ddig_y1iMPqiRaNgKBCY-oQoXyrLT0mLUQBlhHzjJa26yDvW8Ea4kiBuUO378PykqP9CwMHhGDM0huAqffAVnD_UJBe0kwE3OT4N5lNqTtYrhN3z8ZxiS4cNp3zA9P9mbwn46Zs5PtCMawL24t5jPZVV9qzIookzD_SRmXxJlAmF7SnCkvm_RtgvttfP7QKVHPBbJrnCJ1bTYfYeeQQni9S4r-VKhocnxo4a0nzDBtl7GQHJPteXXg4l8kfMWXGSZKq6WD2XAUbhRnx624PVPuvDynF6GTZf-HwS3iyokykSovTo3Gk3QTQaNI_yEbqK6tNzVj3zqSymev2hhV86GcL6bA4YV9-k4YBiIXg6nWdQUxUHws94GOQc4ryWtXADh1Kbdf5HWd6HDSX18ro6EYgNDnWnvsp24r3jaYwLcQjOyHBwF0YcosMjH5M4LJKLNXRnfJf97h3JU_CgLAntonRXqZgHlJL1JVTL684hGwso47EPv5dx8ryafPa3gjIBd_HVbJmkgSqRG_94yCCcBFZIRufeU-M4z59yGb-D-D1v1DYWO7848k0hv5IR3vnsEZe9VntDU2Y5CBYQcCvlLdB5kRQroYakOnNSmyhWfyeOtaqCN-9jXZacoRAZus_9_yr5CX_n0hYj_Z8r-2L-xspZW3iGqLGOVBVmy9vLEJJ0EgYvrDeITtej5h36exE03oFdYsPXZK3X8FmC-6S7Q98AyzLw8tG5AfKI-lNkJybvOrRzjtRMjMcHcGFXWi2-aTe6C9VH-bfe2iL1Z4gIY9P28cEblKPK4jOgE-pjYkO_rnBq0vvYCRob3aB0f9BPnBE4698ck2G7D28bp0bgjIqz6dr8B3RklyBiEbT71iYV48pCOdNpurQdZrahB9_LL6Y_4FadYqKCispRsc_i3rzpoey6i8WNpI1jzTUeR_zsouDAhtYwOWprQ4vH-BEA7VqqOM48t0Zj3XmNlvaK8TFa_RlMFPKePbHWtnYCAckjs-QifysCX_Xu4uZMXdRuyNZHyWJoNv5iG7abSFvQQkaUOhJZjTWHWwbvs0w52UfqAkbAEOWwIpoloeWP_6ujRjvXzgILiwFHeOo6aSDHvSmNqtSv6M26a3TpOaD3CFVeZG9t5Mhm8mNV3Wtkdjb6A4vmjwG1xYSkJxlg-sIYx09ok9UZ5v733fI9VnS1wFHuIAoTKEXsFobRjnVKbk4qk96JYO-pL5D_eGkasv0v_ClW-SbaZObsmrWdjfS1I_4qKDJEgOEXMW5Qq7nRNMNKLCLOep7BS2yR7N5uG8FP-eDaxLrPxeN1NUDe7ViUlcXgsINZVf-AFLF-wFwxIfKrCb3zbUvA9nJWoiLnq4QoaSULXAxahqDblTRYruJ_m57PDWc_4I5Jmcp855S6q13-M2z9yR5buS1nCi_8kiKMkreewCTiZJrXxTE12iBXJlgOwx0HXOXgGlCRk00ReMRYtnnB4TLqmUcxANefadU7QtY7MC3DeFPQ3RCT3LdHQ06pbXE-7XL5cb_H8ymYkskd4pZtj3I9pA_6UtZNOnDjT42cqqXVsUwGCQyCJ_sgftAm2EfFG0pDT6UC6yOYpHIkeaTQb-UIdP47ePcQJWOiRHgofR3yMwh6cYwNW6KPtG69AhRaCugNPTE24D_L2rjIEJRVGUKTK8zPmNF_4FiPADsyT5M7k1Wafxul_yMdGjcdfYOvQ3s77ARVsqhfcMv2uDEvF9IC25rSwkUSuaBkaEBaEO2CXmNpYqjjzEJVSR9g-JGDpV6uU7rL74pEI9Qasixxi7GEZvrwGgLuoB6PZBk0EXhsulJq7fF7Va4uD1Aaa9rbl-oHmjrH_gYz0tCBrUAjXHYcHUuKbIlBEkf6NQRNmmlt9FMWu-AxqzvaT073x0FOBx7Da8SUwzhVlmx-3zYVebJR4LOSricBEqjMCJiUjeohxFpiL-0hXw2uOURzFev78VTw4Sa5BRHHc_CojiZmrRlnALdNSog7OX8OTQ-YjhxLXr3ZcHqJgL5FmhsU7MFzudIGAkJPlt2DBeWhJFcr9Xa0ulezT90m1m9-cdaIWnfnPZYz8yFFIDXoX3EAmrZBpEP6hhxB_3At1wlIuYKhBrkG5xEkOTrDyNXgMkQz4-4ttZMj9QsCp_BALl8RAGQkZHLkGiBEqY1pJ9JPO8HHKEXo1Q57axGYE7bXCMAkJR2G__S6JV6CR7PrOm3I0uU2UTDcDROwrGd0P4QQTmq-mB1G9IhXh63abmxfW9FTqH7nloju0xbuAU5fqkypSrTcWMANshCGwdezceKvRZwinVzaqT0uccO-990mjrzjQBC0dorlxVSd3kWCkAMCT7DtKQ7wSs5sw-KWN_X4Xi-HYNpWP9xIoxF34LU9wrUnVYCcSj8nMZo6zLfoVvXKD-oxRZEAcqAi26JdqhRY0yaSfSKzbKiQfUo1hxs88URLBfjmvQsvM4othXYrUjgBRtfUXMIPLoXQruCeZsTEmdpb5Bu-bu7kEQve6B9Rbx2atA3G8xtF-VieXPWfKWQMfntQ8aOfXmvCV_56f39K_q0gARd0h5x4ujw9i6oUQw0iSPp5b8BZWMRekVXRlOvMOmfzlS_URhyihVF4PDrlCXEHVDzaFVamS8owvaXfzyUB9jaeAkPNwO2VNUc2k3KY7xz0wITz3gL3noBvVMLSeiBL3QYbD9rUjiWpqvRWPJ0cWntvlhnkex-2t78KOG3cdo635LhUx1RiNPqTHu4LQ32p8px19-6x8d_GDlzhWNAumelVUeyj44T4IA9CEkN6AFQ-w2HWgJ3wkBrknsrI9lhd8cOTi6hoSvSS0BFinEjCPNGKPx6T_e-PfLEOstK7c2vDhYnrZ7E8idZx-jorfHrxe80wjwnsoNkFADqFdHJ76RfMxnbuv5tYTIt5ZtrrMdi_pLPXKLVGFGgWZf3X7Nm5PUEcPlOlkOptdWb9LkNH2KaGSvaCRO6OMm83z-7dVifD1j3dXtROkOJzNgduolYlUaT9oP1Y7zHqH8B01ENMs8wAZgyrdDFGqYEJ591O8wJpES4zz_PgJdz9f6gm9zUo7yPwE_H7TbcgABisysnwm2_nPQZ9ViGcu06W2WcrEb04yEQcz95VS5U2L3HXqynyBjVKDHlkVonelMKNOngmvb8VL6lplPMeGx_q4IfhNlhq9jZ_zSh--fWWsjeaIyC7-zACpE7-pDmYUrhqY9QzMl_D1nOtfQOZxRXd2sgH9KiINeJLL9oxjGgg-GsLzLVt93rE8f-phQvLvrh11rvpaANPnNAGOqPbu78zR8K4j1AJA_Wjik84J3WxJyiuSnASFE8EDg0GXYnPgU-5i5nEtPDcDnGBOwAfHlZr_wGTb9kZR5a5tTwurfbcHEoZa7DPVXTRvEQgsYU30hx_ALW1iNWIKKs7opL4BFvlx3tm8sTP732fP9GUzzh3PatGMxfpPOmKE-_YERgk2AvwQx1gnYaspsaXzoEvj2Vl47nfAwATzCylz9O9qNgrNXfHwRFE6tLaBlDR5eYbrClkDJOppSWS3PIME2of5JI9Nznlvnca4oZvcPqHmSm8ST4nYXoPIIzBCtsdUFztXFo1vY8nAOmZRbD1_e1UR8N6TVlUf77kEpDQe0vxlVC_NbAZcNPKyHsX9quRCcZURYjsFhmRzpnBgIjS-rIOSmjZlMNg4vKoV8w4XDyBx_EMoIcHAvHWEF9xIT6YKOZTeAIXf5lh9LkNd1HRFFookfRbPKsC110iXBROY0iQKtqY4SZLNi-n5fpTcL_G2SCKwQOMSSFG74LU7TWoSgBZqQN2rhPkVqExrKO2eNNICbH9hdUCFafArkVMNq1p6a4v9609Lt3rxIdGxXfUEmL6B_5";

            testDecrypt(content);
            
        } catch (Exception e) {
            logger.error("测试失败", e);
        }
    }

    private static void testDecrypt(String content) {
        try {
            System.out.println("=== 测试解密 ===");
            String decrypt = AESUtils.decrypt(content, AES_KEY);
            System.out.println("解密结果: " + decrypt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static void testGetRequest() throws Exception {
        System.out.println("=== 测试GET请求加密解密流程 ===");
        
        // 1. 原始GET请求参数
        JSONObject params = new JSONObject();
        params.put("current", 1);
        params.put("size", 10);
        params.put("startTime", "2024-10-20");
        params.put("endTime", "2024-10-31");
        
        System.out.println("原始参数: " + params.toJSONString());

        // 2. 前端加密
        String encrypted = AESUtils.encrypt(params.toJSONString(), AES_KEY);
        System.out.println("前端加密结果: " + encrypted);
        System.out.println("加密结果长度: " + encrypted.length());
        System.out.println("是否符合Base64格式: " + encrypted.matches("^[A-Za-z0-9+/_-]+={0,2}$"));

        // 3. 模拟后端解密
        String decrypted = AESUtils.decrypt(encrypted, AES_KEY);
        System.out.println("后端解密结果: " + decrypted);
        
        // 4. 验证解密结果
        JSONObject decryptedJson = JSON.parseObject(decrypted);
        System.out.println("解密后是否与原始数据相同: " + params.equals(decryptedJson));
    }

    private static void testPostRequest() throws Exception {
        System.out.println("=== 测试POST请求加密解密流程 ===");
        
        // 1. 原始POST请求体
        JSONObject body = new JSONObject();
        body.put("localDate", "2024-11-19");
        body.put("beginTime", "00:00:00");
        body.put("endTime", "23:59:59");
        body.put("address", "河北省石家庄市裕华区");
        body.put("phone", "15032114528");
        
        System.out.println("原始请求体: " + body.toJSONString());

        // 2. 前端加密
        String encrypted = AESUtils.encrypt(body.toJSONString(), AES_KEY);
        System.out.println("前端加密结果: " + encrypted);

        // 3. 模拟后端解密
        String decrypted = AESUtils.decrypt(encrypted, AES_KEY);
        System.out.println("后端解密结果: " + decrypted);
        
        // 4. 验证解密结果
        JSONObject decryptedJson = JSON.parseObject(decrypted);
        System.out.println("解密后是否与原始数据相同: " + body.equals(decryptedJson));
    }

}