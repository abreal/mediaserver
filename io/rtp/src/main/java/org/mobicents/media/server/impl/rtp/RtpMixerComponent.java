/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
        
package org.mobicents.media.server.impl.rtp;

import org.apache.log4j.Logger;
import org.mobicents.media.server.component.audio.MixerComponent;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfInput;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfOutput;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpMixerComponent extends MixerComponent {
    
    private static final Logger logger = Logger.getLogger(RtpMixerComponent.class);

    private final static int DEFAULT_BUFFER_SIZER = 50;

    // RTP receiver
    private final RtpSource rtpSource;
    private final DtmfInput dtmfInput;
    
    private final JitterBuffer jitterBuffer;

    // RTP transmitter
    private final RtpSink rtpSink;
    private final DtmfOutput dtmfOutput;

    // RTP statistics
    private volatile int rxPackets = 0;

    public RtpMixerComponent(int connectionId, RtpClock rtpClock, RtpClock oobClock, Scheduler scheduler, RTPFormats formats) {
        super(connectionId);

        // RTP receiver
        this.jitterBuffer = new JitterBuffer(rtpClock, DEFAULT_BUFFER_SIZER);
        this.rtpSource = new RtpSource(scheduler, jitterBuffer, dsp);
        this.dtmfInput = new DtmfInput(scheduler, oobClock);
        
        // RTP transmitter
        this.rtpSink = new RtpSink(scheduler, dsp);
        this.dtmfOutput = new DtmfOutput(scheduler, transmitter);
    }

    public void setRtpFormats(RTPFormats formats) {
        this.jitterBuffer.setFormats(formats);
    }

    public void processRtpPacket(RtpPacket packet, RTPFormat format) {
        if (this.rxPackets == 0) {
            logger.info("Restarting jitter buffer");
            this.jitterBuffer.restart();
        }
        this.jitterBuffer.write(packet, format);
    }

    public void processDtmfPacket(RtpPacket packet) {
        this.dtmfInput.write(packet);
    }

    public void activate() {
        this.rtpInput.activate();
        this.dtmfInput.activate();
    }

    public void deactivate() {
        this.rtpInput.deactivate();
        this.dtmfInput.deactivate();
        this.dtmfInput.reset();
    }

}
