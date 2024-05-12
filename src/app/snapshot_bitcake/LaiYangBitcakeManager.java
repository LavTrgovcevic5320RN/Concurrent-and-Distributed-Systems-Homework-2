package app.snapshot_bitcake;

import app.AppConfig;
import app.ServentInfo;
import app.SnapshotIndicator;
import servent.message.Message;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.snapshot.LYTellMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class LaiYangBitcakeManager implements BitcakeManager {

	private final AtomicInteger currentAmount = new AtomicInteger(1000);

	private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();

	private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

	private Map<SnapshotIndicator, Map<Integer, Integer>> giveHistories = new ConcurrentHashMap<>();

	private Map<SnapshotIndicator, Map<Integer, Integer>> getHistories = new ConcurrentHashMap<>();

	private int initiatorId = -1;

	private ServentInfo parentNode = null;

	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}
	
	public void addSomeBitcakes(int amount) {
		currentAmount.getAndAdd(amount);
	}
	
	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}

	public LaiYangBitcakeManager() {
		for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			giveHistory.put(neighbor, 0);
			getHistory.put(neighbor, 0);
		}

		for (Integer initId : AppConfig.initiatorIds) {
			giveHistories.computeIfAbsent(new SnapshotIndicator(initId, AppConfig.initiators.get(0)),
					k -> new ConcurrentHashMap<>());
			getHistories.computeIfAbsent(new SnapshotIndicator(initId, AppConfig.initiators.get(0)),
					k -> new ConcurrentHashMap<>());
			AppConfig.myServentInfo.getNeighbors().forEach(neighbor -> {
				giveHistories.get(new SnapshotIndicator(initId, AppConfig.initiators.get(0))).putIfAbsent(neighbor, 0);
				getHistories.get(new SnapshotIndicator(initId, AppConfig.initiators.get(0))).putIfAbsent(neighbor, 0);
			});
		}
	}
	
	/*
	 * This value is protected by AppConfig.colorLock.
	 * Access it only if you have the blessing.
	 */
	public int recordedAmount = 0;
	
	public void markerEvent(int collectorId, SnapshotCollector snapshotCollector, int snapshotId, ServentInfo serventInfo) {
		synchronized (AppConfig.colorLock) {
			int oldVersion = AppConfig.initiators.get(collectorId);
			AppConfig.initiators.put(collectorId, snapshotId);
			giveHistories.computeIfAbsent(new SnapshotIndicator(collectorId, AppConfig.initiators.get(collectorId)),
					k -> new ConcurrentHashMap<>());
			getHistories.computeIfAbsent(new SnapshotIndicator(collectorId, AppConfig.initiators.get(collectorId)),
					k -> new ConcurrentHashMap<>());
			AppConfig.myServentInfo.getNeighbors().forEach(neighbor -> {
				giveHistories.get(new SnapshotIndicator(collectorId, AppConfig.initiators.get(collectorId))).putIfAbsent(neighbor, 0);
				getHistories.get(new SnapshotIndicator(collectorId, AppConfig.initiators.get(collectorId))).putIfAbsent(neighbor, 0);
			});

			recordedAmount = getCurrentBitcakeAmount();

			LYSnapshotResult snapshotResult = new LYSnapshotResult(
					AppConfig.myServentInfo.getId(), recordedAmount, giveHistories.get(new SnapshotIndicator(collectorId, oldVersion)),
					getHistories.get(new SnapshotIndicator(collectorId, oldVersion)));

			if (collectorId == AppConfig.myServentInfo.getId()) {
				snapshotCollector.addLYSnapshotInfo(
						AppConfig.myServentInfo.getId(),
						snapshotResult);

				initiatorId = AppConfig.myServentInfo.getId();
			} else {
			
				Message tellMessage = new LYTellMessage(
						AppConfig.myServentInfo, AppConfig.getInfoById(collectorId), snapshotResult);
				
				MessageUtil.sendMessage(tellMessage);

				if(initiatorId != -1) {
					AppConfig.timestampedStandardPrint("Initiator already exists = " + initiatorId);
				} else {
					initiatorId = collectorId;
					parentNode = serventInfo;
					if (parentNode != null) {
						AppConfig.timestampedStandardPrint("For node " + AppConfig.myServentInfo.getId() + " parent is " + parentNode.getId() + " and initiator is " + initiatorId);
					}
				}
			}
			
			for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				if (neighbor.equals(collectorId) && serventInfo != null && neighbor.equals(serventInfo.getId()))
					continue;
				Message clMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId);
				MessageUtil.sendMessage(clMarker);
				try {
					/*
					 * This sleep is here to artificially produce some white node -> red node messages.
					 * Not actually recommended, as we are sleeping while we have colorLock.
					 */
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {
		
		private int valueToAdd;
		
		public MapValueUpdater(int valueToAdd) {
			this.valueToAdd = valueToAdd;
		}
		
		@Override
		public Integer apply(Integer key, Integer oldValue) {
			return oldValue + valueToAdd;
		}
	}
	
	public void recordGiveTransaction(int neighbor, int amount) {
		giveHistory.compute(neighbor, new MapValueUpdater(amount));
	}
	
	public void recordGetTransaction(int neighbor, int amount) {
		getHistory.compute(neighbor, new MapValueUpdater(amount));
	}

	public void recordGiveTransaction(SnapshotIndicator snapshotIndicator, int neighbor, int amount) {
		giveHistories.get(snapshotIndicator).compute(neighbor, new MapValueUpdater(amount));
	}

	public void recordGetTransaction(SnapshotIndicator snapshotIndicator, int neighbor, int amount) {
		getHistories.get(snapshotIndicator).compute(neighbor, new MapValueUpdater(amount));
	}
}
