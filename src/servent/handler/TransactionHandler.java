package servent.handler;

import app.AppConfig;
import app.SnapshotIndicator;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import servent.message.Message;
import servent.message.MessageType;

public class TransactionHandler implements MessageHandler {

	private Message clientMessage;
	private BitcakeManager bitcakeManager;
	
	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			String amountString = clientMessage.getMessageText();
			
			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}
			
			bitcakeManager.addSomeBitcakes(amountNumber);
			synchronized (AppConfig.colorLock) {
				if (bitcakeManager instanceof LaiYangBitcakeManager) {
					LaiYangBitcakeManager lyFinancialManager = (LaiYangBitcakeManager)bitcakeManager;
					
//					lyFinancialManager.recordGetTransaction(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
					for (SnapshotIndicator snapshotIndicator : clientMessage.getSnapshotIndicators()) {
						lyFinancialManager.recordGetTransaction(snapshotIndicator, clientMessage.getOriginalSenderInfo().getId(),
								amountNumber);
					}
				}
			}
		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}

}
