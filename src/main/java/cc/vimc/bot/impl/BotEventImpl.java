package cc.vimc.bot.impl;

import cc.vimc.bot.dao.MessageDAO;
import cc.vimc.bot.dao.UserDAO;
import cc.vimc.bot.dto.BotRequestDTO;

import cc.vimc.bot.dto.UserDTO;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import javax.annotation.Resource;
import java.util.Random;
import java.util.stream.IntStream;

import static cc.vimc.bot.enums.Commands.*;
import static cc.vimc.bot.enums.Fields.GROUP;
import static cc.vimc.bot.enums.Fields.PRIVATE;


@Service
public class BotEventImpl {

    private static final Logger logger = LoggerFactory.getLogger(BotEventImpl.class);
    @Autowired
    BotApiImpl botApi;

    @Autowired
    MinecraftImpl minecraft;

    @Resource
    MessageDAO messageDAO;

    @Value("${MASTER_QQ}")
    String masterQQ;

    @Value("${MC_GROUP_QQ}")
    String mcGroupQQ;


    public void botEventHandle(String request) {
        var botRequestDTO = new BotRequestDTO();
        try {
            botRequestDTO = JSON.parseObject(request, BotRequestDTO.class);
        } catch (Exception e) {
            logger.error("模型转换出错！：{}", e);
            return;
        }
        var messageType = botRequestDTO.getMessage_type();
        var message = botRequestDTO.getMessage();
        if (anyEvent(botRequestDTO)) {
            return;
        }
        if (!StringUtils.isEmpty(message)) {
            switch (messageType) {
                //处理组消息
                case GROUP:
                    groupEvent(botRequestDTO);
                    break;
                //处理私有消息
                case PRIVATE:
                    privateEvent(botRequestDTO);
                    break;
                default:
                    break;
            }
        }

    }

    /**
     * @param botRequestDTO
     * @return void
     * @Description 处理任意消息
     * @author wlwang3
     * @date 2019/3/14
     */
    private boolean anyEvent(BotRequestDTO botRequestDTO) {
        var message = botRequestDTO.getMessage();

        switch (message) {
            case HELP:
                var sendMessage = new StringBuilder();
                ALL_COMMANDS.forEach((k, v) -> sendMessage.append(k).append(" ").append(v).append("\n"));
                botApi.sendMsg(botRequestDTO, sendMessage.toString().substring(0, sendMessage.length() - 1));
                return true;
            case FUCK:
                var huaQ = new StringBuilder();
                var random = new Random(System.currentTimeMillis());
                var randomInt = random.nextInt(20) % 10;
                IntStream.range(0, randomInt).forEach(value -> huaQ.append(ALL_COMMANDS.get(FUCK)));
                botApi.sendMsg(botRequestDTO, huaQ.toString());
                return true;
            default:
                return false;
        }
    }

    /**
     * @param botRequestDTO bot请求模型
     * @return void
     * @Description 处理私有消息
     * @author wlwang3
     * @date 2019/3/14
     */
    private void privateEvent(BotRequestDTO botRequestDTO) {

        var message = botRequestDTO.getMessage();
        var userId = botRequestDTO.getSender().getUser_id();
        // 这里处理主人QQ消息 //是权限更高的命令
        if (masterQQ.contains(userId)) {
            if (message.startsWith("//")) {
                botApi.sendMsg(botRequestDTO, minecraft.sendCommand(message.substring(2)));
            }
        }
    }

    /**
     * @param botRequestDTO
     * @return void
     * @Description 处理组消息
     * @author wlwang3
     * @date 2019/3/14
     */
    private void groupEvent(BotRequestDTO botRequestDTO) {
        var message = botRequestDTO.getMessage();
        var nickName = botRequestDTO.getSender().getNickname();
        if (messageDAO.repeatMessageCount(botRequestDTO.getGroup_id(),message)){
            botApi.sendMsg(botRequestDTO,message);
        }
        //处理MCQQ群里的消息
        if (mcGroupQQ.equals(botRequestDTO.getGroup_id())) {
            //命令
            switch (message) {
                case LIST:
                    minecraft.postPlayerList(nickName);
                    break;
                case TPS:
                    botApi.sendMsg(botRequestDTO, minecraft.sendCommand(TPS.substring(1)));
                    break;
                case POINTS_LEAD:
                    botApi.sendMsg(botRequestDTO, minecraft.sendCommand(POINTS_LEAD.substring(1)));
                    break;
                default:
                    break;
            }
        }
    }
}
