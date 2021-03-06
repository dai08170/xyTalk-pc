package xysoft.im.adapter;

import xysoft.im.app.Launcher;
import xysoft.im.cache.UserCache;
import xysoft.im.components.Colors;
import xysoft.im.db.model.CurrentUser;
import xysoft.im.db.service.ContactsUserService;
import xysoft.im.db.service.CurrentUserService;
import xysoft.im.components.UserInfoPopup;
import xysoft.im.listener.AbstractMouseListener;
import xysoft.im.utils.AvatarUtil;
import xysoft.im.utils.DebugUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;


public class RoomMembersAdapter extends BaseAdapter<RoomMembersItemViewHolder>
{
    private List<String> members;
    private List<RoomMembersItemViewHolder> viewHolders = new ArrayList<>();
    private CurrentUserService currentUserService = Launcher.currentUserService;
    private ContactsUserService contactsUserService = Launcher.contactsUserService;
    private MouseAdapter addMemberButtonMouseListener;
    private MouseAdapter removeMemberButtonMouseListener;


    public RoomMembersAdapter(List<String> members)
    {
        this.members = members;
    }

    @Override
    public RoomMembersItemViewHolder onCreateViewHolder(int viewType)
    {
        return new RoomMembersItemViewHolder();
    }

    @Override
    public void onBindViewHolder(RoomMembersItemViewHolder viewHolder, int position)
    {
        if (!viewHolders.contains(viewHolder))
        {
            viewHolders.add(viewHolder);
        }

        String name = members.get(position);

        if (name.equals("添加成员") || name.equals("删除成员") ){
            viewHolder.roomName.setText(name);        	
        }
        else{
            String realName = Launcher.contactsUserService.findByUsername(name)==null?"未知":Launcher.contactsUserService.findByUsername(name).getName();
            viewHolder.roomName.setText(realName +" - " + name);       	
        }


        if (name.equals("添加成员"))
        {
            viewHolder.setCursor(new Cursor(Cursor.HAND_CURSOR));

            ImageIcon imageIcon = new ImageIcon(getClass().getResource("/image/add_member.png"));
            imageIcon.setImage(imageIcon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
            viewHolder.avatar.setIcon(imageIcon);

            viewHolder.addMouseListener(new AbstractMouseListener()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    //System.out.println("添加/刪除用戶");
                    //selectAndAddRoomMember();
                    if (addMemberButtonMouseListener != null)
                    {
                        addMemberButtonMouseListener.mouseClicked(e);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e)
                {
                    viewHolder.setBackground(Colors.ITEM_SELECTED_LIGHT);
                    super.mouseEntered(e);
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    viewHolder.setBackground(Colors.WINDOW_BACKGROUND_LIGHT);

                }
            });
        } else if (name.equals("删除成员"))
        {
            viewHolder.setCursor(new Cursor(Cursor.HAND_CURSOR));

            ImageIcon imageIcon = new ImageIcon(getClass().getResource("/image/delete_member.png"));
            imageIcon.setImage(imageIcon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
            viewHolder.avatar.setIcon(imageIcon);

            viewHolder.addMouseListener(new AbstractMouseListener()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    viewHolder.setBackground(Colors.ITEM_SELECTED_LIGHT);
                    super.mouseEntered(e);
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    viewHolder.setBackground(Colors.WINDOW_BACKGROUND_LIGHT);

                }

                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (removeMemberButtonMouseListener != null)
                    {
                        removeMemberButtonMouseListener.mouseClicked(e);
                    }
                }
            });
        } else
        {
            ImageIcon imageIcon = new ImageIcon();
            imageIcon.setImage(AvatarUtil.createOrLoadUserAvatar(name).getScaledInstance(30, 30, Image.SCALE_SMOOTH));
            viewHolder.avatar.setIcon(imageIcon);

            UserInfoPopup userInfoPopup = new UserInfoPopup(name);


            if (!name.equals(UserCache.CurrentUserName))
            {
                viewHolder.addMouseListener(new AbstractMouseListener()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        viewHolder.setBackground(Colors.ITEM_SELECTED_LIGHT);

                        // 弹出用户信息面板
                        if (e.getButton() == MouseEvent.BUTTON1)
                        {
                            userInfoPopup.show(e.getComponent(), e.getX(), e.getY());
                        }


                        for (RoomMembersItemViewHolder holder : viewHolders)
                        {
                            if (holder != viewHolder)
                            {
                                holder.setBackground(Colors.WINDOW_BACKGROUND_LIGHT);
                            }
                        }

                    }
                });
            }
        }

    }


    @Override
    public int getCount()
    {
        return members.size();
    }

    public void setAddMemberButtonMouseListener(MouseAdapter addMemberButtonMouseListener)
    {
        this.addMemberButtonMouseListener = addMemberButtonMouseListener;
    }

    public void setRemoveMemberButtonMouseListener(MouseAdapter removeMemberButtonMouseListener)
    {
        this.removeMemberButtonMouseListener = removeMemberButtonMouseListener;
    }
}
