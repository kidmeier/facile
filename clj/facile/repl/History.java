package clj.facile.repl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

public class History implements Iterable<History.Item> {

	public class Item {
		public Calendar when;
		public ReplServer.Evaluation eval;
		
		public Item(final ReplServer.Evaluation eval, final Calendar when) {
			this.eval = eval;
			this.when = when;
		}
	}
	
	private List<SoftReference<Item>> history;
	private ReferenceQueue<Item> collectedItems;
	
	public History() {
		this.history = new ArrayList<SoftReference<Item>>();
		this.collectedItems = new ReferenceQueue<Item>();
	}

	private void cleanupRefs() {
		
		int count = 0;
		
		// Remove anything lingering in the reference queue
		Reference<? extends Item> collectedRef = this.collectedItems.poll();
		while( null != collectedRef ) {
			this.history.remove(collectedRef);
			collectedRef = this.collectedItems.poll();
			count++;
		}
		if( count > 0 ) {
			System.out.println(getClass().getName() + ": Removed " + count + " refs.");
			System.out.println(getClass().getName() + ": " + this.history.size() + " items remaining");
		}
	}
	
	public int size() {
		cleanupRefs();
		
		return this.history.size();
	}
	
	public int enqueue(ReplServer.Evaluation eval) {
		return this.enqueue(new Item(eval, Calendar.getInstance()));
	}
	
	public int enqueue(Item item) {
		this.history.add( new SoftReference<Item>(item,this.collectedItems) );
		return this.history.size();
	}
	
	public Item get(int i) {
		
		if( i < 0 || i >= this.history.size() )
			return null;
		
		final SoftReference<Item> ref = this.history.get(i);
		final Item item = ref.get();
		
		// Reference has been garbage collected?
		if( null == item ) {
			
			this.cleanupRefs();
			// Item is unavailable
			return null;
		}
		
		return item;
	}

	public List<Item> list() {
		
		// Clean up collected references.
		cleanupRefs();
		
		final ArrayList<Item> list = new ArrayList<Item>(this.history.size());
		for( Reference<Item> r : this.history ) {
			list.add( r.get() );
		}
		return list;
	}
	
	public Iterator<Item> iterator() {
		return list().iterator();
	}
}
