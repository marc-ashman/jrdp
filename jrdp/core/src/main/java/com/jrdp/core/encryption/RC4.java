package com.jrdp.core.encryption;

import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

class RC4
{
	private RC4Engine engine;
	private int uses;
	private int maxUses;
	private KeyUpdateRequestListener listener;
	
	public RC4(byte[] key, int usesBeforeUpdate, KeyUpdateRequestListener listener)
	{
		updateKey(key);
		maxUses = usesBeforeUpdate;
		uses = 0;
		this.listener = listener;
	}

	public byte[] decrypt(byte[] data, int offset, int length) {
		return process(data, offset, length);
	}

	public byte[] encrypt(byte[] data, int offset, int length) {
		return process(data, offset, length);
	}

	public void warnOfUse() {
		uses++;
		if(uses >= maxUses)
		{
			listener.onKeyUpdateRequest(this);
		}
	}
	
	public synchronized void updateKey(byte[] newKey)
	{
		engine = new RC4Engine();
		engine.init(true, new KeyParameter(newKey));
		uses = 0;
	}
	
	public synchronized byte[] process(byte[] data, int offset, int length)
	{
		engine.processBytes(data, offset, length, data, offset);
		return data;
	}
	
	public interface KeyUpdateRequestListener
	{
		public void onKeyUpdateRequest(RC4 source);
	}
}
