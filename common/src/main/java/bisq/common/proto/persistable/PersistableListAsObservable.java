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

package bisq.common.proto.persistable;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.Collection;
import java.util.List;

public abstract class PersistableListAsObservable<T extends PersistablePayload> extends PersistableList<T> {

    public PersistableListAsObservable() {
    }

    protected PersistableListAsObservable(Collection<T> collection) {
        super(collection);
    }

    protected List<T> createList() {
        return FXCollections.observableArrayList();
    }

    public ObservableList<T> getObservableList() {
        return (ObservableList<T>) getList();
    }

    public void addListener(ListChangeListener<T> listener) {
        ((ObservableList<T>) getList()).addListener(listener);
    }

    public void removeListener(ListChangeListener<T> listener) {
        ((ObservableList<T>) getList()).removeListener(listener);
    }
}
