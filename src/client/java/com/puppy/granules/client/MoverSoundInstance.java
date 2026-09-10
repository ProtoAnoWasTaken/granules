package com.puppy.granules.client;

import com.puppy.granules.entity.MoverEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class MoverSoundInstance extends AbstractTickableSoundInstance {
	private final MoverEntity mover;
	private final boolean underwater;

	public MoverSoundInstance(MoverEntity mover, SoundEvent sound, boolean underwater) {
		super(sound, SoundSource.BLOCKS, RandomSource.create());
		this.mover = mover;
		this.underwater = underwater;
		this.looping = true;
		this.volume = 0.35F;
		this.pitch = 0.8F;
		this.x = mover.getX();
		this.y = mover.getY();
		this.z = mover.getZ();
	}

	@Override
	public void tick() {
		if (this.mover.isRemoved() || this.mover.getMoverSpeed() <= 0.0F || this.mover.isMovingThroughFluid() != this.underwater) {
			this.stop();
			return;
		}
		this.x = this.mover.getX();
		this.y = this.mover.getY();
		this.z = this.mover.getZ();
		this.volume = Math.min(0.65F, 0.25F + this.mover.getMoverSpeed() * 0.3F);
		this.pitch = Math.min(1.2F, 0.75F + this.mover.getMoverSpeed() * 0.25F);
	}

	public boolean isUnderwater() {
		return this.underwater;
	}

	public void end() {
		this.stop();
	}
}
