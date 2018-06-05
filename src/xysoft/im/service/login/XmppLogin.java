package xysoft.im.service.login;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException.NotAMucServiceException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.packet.MUCUser.Invite;
import org.jivesoftware.smackx.muclight.MultiUserChatLightManager;
import org.jivesoftware.smackx.push_notifications.PushNotificationsManager;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import xysoft.im.app.Launcher;
import xysoft.im.cache.UserCache;
import xysoft.im.constant.Res;
import xysoft.im.db.model.ContactsUser;
import xysoft.im.db.model.Room;
import xysoft.im.listener.SessionManager;
import xysoft.im.service.ChatService;
import xysoft.im.service.ErrorMsgService;
import xysoft.im.service.FormService;
import xysoft.im.service.HeadlineChatService;
import xysoft.im.service.MucChatService;
import xysoft.im.service.ProviderRegister;
import xysoft.im.service.StateService;
import xysoft.im.service.XmppFileService;
import xysoft.im.swingDemo.SimulationUserData;
import xysoft.im.utils.DebugUtil;


/**
 * XMPP验证登陆
 *
 */
public class XmppLogin implements Login {

	String username;
	String password;
	String tag;

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public XmppLogin() {
		super();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String login() {

		if (Launcher.IS_DEBUGMODE) {
			SmackConfiguration.DEBUG = true;
		}

		try {
			XMPPTCPConnectionConfiguration conf = retrieveConnectionConfiguration();
			if (conf == null) {
				return "XMPPTCPConnectionConfiguration is null";
			}

			Launcher.connection = new XMPPTCPConnection(conf);

			// 连接监听
			SessionManager sessionManager = new SessionManager();
			Launcher.connection.addConnectionListener(sessionManager);

			// 重连管理
			ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(Launcher.connection);
			reconnectionManager.setFixedDelay(Res.RE_CONNET_INTERVAL);// 重联间隔
			reconnectionManager.enableAutomaticReconnection();// 开启重联机制

			StanzaFilter filterMsg = new StanzaTypeFilter(Message.class);
//			StanzaFilter filterIQ = new StanzaTypeFilter(IQ.class);
			// PacketCollector myCollector =
			// Launcher.connection.createPacketCollector(filterMsg);
			StanzaListener listenerMsg = new StanzaListener() {
				@Override
				public void processStanza(Stanza stanza)
						throws NotConnectedException, InterruptedException, NotLoggedInException {
					DebugUtil.debug("processPacket:" + stanza.toXML());
					if (stanza instanceof org.jivesoftware.smack.packet.Message) {
						ChatService.messageProcess(stanza);
					}
				}
				
			};

//			StanzaListener listenerIQ = new StanzaListener() {
//
//				@Override
//				public void processStanza(Stanza stanza)
//						throws NotConnectedException, InterruptedException, NotLoggedInException {
//
//					if (stanza instanceof IQ) {
//						// TODO IQ包
//						
//					}
//				}
//			};

			//加入异步监听
			Launcher.connection.addAsyncStanzaListener(listenerMsg, filterMsg);
			//Launcher.connection.addSyncStanzaListener(listenerIQ, filterIQ);

			try {
				Launcher.connection.connect();
				Launcher.connection.login();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 注册扩展
			ProviderRegister.register();

			sessionManager.initializeSession(Launcher.connection);
			sessionManager.setUserBareAddress(getUsername() + "@" + Launcher.DOMAIN);
			sessionManager.setJID(sessionManager.getUserBareAddress() + "/" + Launcher.RESOURCE);
			sessionManager.setUsername(username);
			sessionManager.setPassword(password);
			
			//注册MUC邀请监听
			MucChatService.mucInvitation();
			//MucChatService.mucGetInfo("a8@muc.win7-1803071731");


			//接收文件listener
			final FileTransferManager manager = FileTransferManager.getInstanceFor(Launcher.connection);
			
			manager.addFileTransferListener(XmppFileService.fileListener());

			UserCache.CurrentUserName = sessionManager.getUsername();
			UserCache.CurrentUserRealName = Launcher.contactsUserService.findByUsername(UserCache.CurrentUserName).getName();
			UserCache.CurrentUserPassword = sessionManager.getPassword();
			UserCache.CurrentUserToken = "";
			UserCache.CurrentBareJid = sessionManager.getUserBareAddress();
			UserCache.CurrentFullJid = sessionManager.getJID();

			DebugUtil.debug("UserCache.CurrentUserName:" + UserCache.CurrentUserName);
			DebugUtil.debug("UserCache.CurrentBareJid:" + UserCache.CurrentBareJid);
			DebugUtil.debug("UserCache.CurrentFullJid:" + UserCache.CurrentFullJid);

			DebugUtil.debug("Launcher.connection.getUser():" + Launcher.connection.getUser());
			DebugUtil.debug("Launcher.connection.getServiceName():" + Launcher.connection.getServiceName());

			// Chat chat = ChatManager.getInstanceFor(Launcher.connection)
			// .createChat("test1@win7-1803071731");
			//
			// ChatManager chatManager =
			// ChatManager.getInstanceFor(Launcher.connection);
			//
			// chatManager.addChatListener(new ChatManagerListener() {
			//
			// @Override
			// public void chatCreated(Chat cc, boolean bb) {
			// // 当收到来自对方的消息时触发监听函数
			// cc.addMessageListener(new ChatMessageListener() {
			// @Override
			// public void processMessage(Chat cc, Message mm) {
			// System.out.println("app2收到消息:" + cc + mm.getBody());
			// }
			// });
			// }
			// });
			//
			// Message message = new Message();
			// message.addExtension(new Receipt());
			// message.setBody("你好！");
			// chat.sendMessage(message);

			// 推送服务
			// PushNotificationsManager pushNotificationsManager =
			// PushNotificationsManager.getInstanceFor(Launcher.connection);
			//
			// try {
			// boolean isSupported =
			// pushNotificationsManager.isSupported();//.isSupportedByServer();
			// boolean isSupportedByServer =
			// pushNotificationsManager.isSupportedByServer();//.isSupportedByServer();
			//
			// DebugUtil.debug("PushNotificationsManager isSupported:"
			// +isSupported);
			// DebugUtil.debug("PushNotificationsManager isSupportedByServer:"
			// +isSupportedByServer);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			


			MucChatService.joinAllRooms();
			
			if (Launcher.connection.isAuthenticated())
				return "ok";
			else
				return "no authenticated";

		} catch (XMPPException e) {
			e.printStackTrace();
			return "XMPPException:" + e.getMessage();

		} catch (SmackException e) {
			e.printStackTrace();
			return "SmackException:" + e.getMessage();

		} catch (IOException e) {
			e.printStackTrace();
			return "IOException:" + e.getMessage();

		} catch (InterruptedException e) {
			e.printStackTrace();
			return "InterruptedException:" + e.getMessage();
		}

	}

	

	protected XMPPTCPConnectionConfiguration retrieveConnectionConfiguration() {

		try {

			DebugUtil.debug("login:" + getUsername() + "--" + getPassword() + "--" + Launcher.HOSTPORT + "--"
					+ Launcher.DOMAIN + "--" + InetAddress.getByName(Launcher.HOSTNAME));

			XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder()
					.setUsernameAndPassword(getUsername(), getPassword()).setResource(Launcher.RESOURCE)
					.setPort(Launcher.HOSTPORT).setConnectTimeout(5000).setXmppDomain(Launcher.DOMAIN)
					.setHost(Launcher.HOSTNAME).setHostAddress(InetAddress.getByName(Launcher.HOSTNAME)) .setSecurityMode(SecurityMode.disabled)
					.setDebuggerEnabled(true);
			DebugUtil.debug("builder:" + builder.toString());
			return builder.build();

		} catch (Exception e) {

			e.printStackTrace();
		}
		return null;
	}

}
