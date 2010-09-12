/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MaplePet;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleInventoryType;
import net.world.MaplePartyCharacter;

/**
 * @author TheRamon
 */
public final class PetLootHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MaplePet pet = c.getPlayer().getPet(c.getPlayer().getPetIndex(slea.readInt()));
        if (!pet.isSummoned()) return;
        
        slea.skip(13);
        int oid = slea.readInt();
        MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
        if (ob == null || pet == null) {
            c.announce(MaplePacketCreator.getInventoryFull());
            return;
        }
        if (ob instanceof MapleMapItem) {
            MapleMapItem mapitem = (MapleMapItem) ob;
            synchronized (mapitem) {
                if (mapitem.isPickedUp()) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    return;
                }
                if (mapitem.getDropper() == c.getPlayer()) {
                    return;
                }
                if (mapitem.getMeso() > 0) {
                        if (c.getPlayer().getParty() != null) {
                            int mesosamm = mapitem.getMeso();
                            if (mesosamm > 50000 * c.getPlayer().getMesoRate()) {
                                return;
                            }
                            int partynum = 0;
                            for (MaplePartyCharacter partymem : c.getPlayer().getParty().getMembers()) {
                                if (partymem.isOnline() && partymem.getMapid() == c.getPlayer().getMap().getId() && partymem.getChannel() == c.getChannel()) {
                                    partynum++;
                                }
                            }
                            for (MaplePartyCharacter partymem : c.getPlayer().getParty().getMembers()) {
                                if (partymem.isOnline() && partymem.getMapid() == c.getPlayer().getMap().getId()) {
                                    MapleCharacter somecharacter = c.getChannelServer().getPlayerStorage().getCharacterById(partymem.getId());
                                    if (somecharacter != null) {
                                        somecharacter.gainMeso(mesosamm / partynum, true, true, false);
                                    }
                                }
                            }
                        } else if (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).findById(1812000) != null) {
                        c.getPlayer().gainMeso(mapitem.getMeso(), true, true, false);
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, c.getPlayer().getPetIndex(pet)), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        mapitem.setPickedUp(false);
                        c.announce(MaplePacketCreator.enableActions());
                        return;
                    }
                } else if (ItemPickupHandler.useItem(c, mapitem.getItem().getItemId())) {
                    if (mapitem.getItem().getItemId() / 10000 == 238) {
                        c.getPlayer().getMonsterBook().addCard(c, mapitem.getItem().getItemId());
                    }
                    mapitem.setPickedUp(true);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                    c.getPlayer().getMap().removeMapObject(ob);
                } else if (mapitem.getItem().getItemId() / 100 == 50000) {
                    if (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).findById(1812007) != null) {
                        for (int i : c.getPlayer().getExcluded()) {
                            if (mapitem.getItem().getItemId() == i) {
                                return;
                            }
                        }
                    } else if (MapleInventoryManipulator.addById(c, mapitem.getItem().getItemId(), mapitem.getItem().getQuantity(), null, null, mapitem.getItem().getExpiration())) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, c.getPlayer().getPetIndex(pet)), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        return;
                    }
                } else if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, c.getPlayer().getPetIndex(pet)), mapitem.getPosition());
                    c.getPlayer().getMap().removeMapObject(ob);
                } else {
                    return;
                }
                mapitem.setPickedUp(true);
            }
        }
        c.announce(MaplePacketCreator.enableActions());
    }
}
