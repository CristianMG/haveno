/*
 * This file is part of Haveno.
 *
 * Haveno is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Haveno is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Haveno. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.account.witness;

import bisq.network.p2p.storage.persistence.PersistableNetworkPayloadStore;

import com.google.protobuf.Message;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;


/**
 * We store only the payload in the PB file to save disc space. The hash of the payload can be created anyway and
 * is only used as key in the map. So we have a hybrid data structure which is represented as list in the protobuffer
 * definition and provide a hashMap for the domain access.
 */
@Slf4j
public class AccountAgeWitnessStore extends PersistableNetworkPayloadStore<AccountAgeWitness> {

    public AccountAgeWitnessStore() {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    private AccountAgeWitnessStore(List<AccountAgeWitness> list) {
        super(list);
    }

    public Message toProtoMessage() {
        return protobuf.PersistableEnvelope.newBuilder()
                .setAccountAgeWitnessStore(getBuilder())
                .build();
    }

    private protobuf.AccountAgeWitnessStore.Builder getBuilder() {
        final List<protobuf.AccountAgeWitness> protoList = map.values().stream()
                .map(payload -> (AccountAgeWitness) payload)
                .map(AccountAgeWitness::toProtoAccountAgeWitness)
                .collect(Collectors.toList());
        return protobuf.AccountAgeWitnessStore.newBuilder().addAllItems(protoList);
    }

    public static AccountAgeWitnessStore fromProto(protobuf.AccountAgeWitnessStore proto) {
        List<AccountAgeWitness> list = proto.getItemsList().stream()
                .map(AccountAgeWitness::fromProto).collect(Collectors.toList());
        return new AccountAgeWitnessStore(list);
    }
}
