// Copyright (c) 2012-2015 Saarland University
// All rights reserved.
//
// Author: Konrad Jamrozik, jamrozik@st.cs.uni-saarland.de
//
// This file is part of the "DroidMate" project.
//
// www.droidmate.org

package org.droidmate.device

import groovy.util.logging.Slf4j
import org.droidmate.exceptions.DeviceException
import org.droidmate.exceptions.DeviceNeedsRebootException
import org.droidmate.exceptions.TcpServerUnreachableException

@Slf4j
public class SerializableTCPClient<InputToServerT extends Serializable, OutputFromServerT extends Serializable> implements ISerializableTCPClient<InputToServerT, OutputFromServerT>
{

  private final String serverAddress = "localhost"
  private final int    socketTimeout


  public SerializableTCPClient(int socketTimeout)
  {
    this.socketTimeout = socketTimeout
  }


  /**
   * Sends through TCP socket the serialized {@code input} to server under {@link #serverAddress}:{@code port}.<br/>
   * Next, waits until server returns his answer and returns it.
   */
  @SuppressWarnings("unchecked")
  public OutputFromServerT queryServer(InputToServerT input, int port) throws DeviceNeedsRebootException, TcpServerUnreachableException, DeviceException
  {

    OutputFromServerT output
    try
    {
      log.trace("Socket socket = new Socket($serverAddress, $port)")

      Socket socket = this.tryGetSocket(serverAddress, port)

      socket.soTimeout = this.socketTimeout

      ObjectInputStream inputStream

      try
      {
        // This will block until corresponding socket output stream (located on server) is flushed.
        //
        // Reference:
        // 1. the ObjectInputStream constructor comment.
        // 2. search for: "Note - The ObjectInputStream constructor blocks until" in:
        // http://docs.oracle.com/javase/7/docs/platform/serialization/spec/input.html
        //
        inputStream = new ObjectInputStream(socket.inputStream)
      } catch (EOFException e)
      {
        throw new TcpServerUnreachableException(e)
      } catch (SocketTimeoutException e)
      {
        throw new TcpServerUnreachableException(e)
      }
      assert inputStream != null

      ObjectOutputStream outputStream
      try
      {
        outputStream = new ObjectOutputStream(socket.outputStream)
      } catch (EOFException e)
      {
        throw new TcpServerUnreachableException(e)
      }
      assert outputStream != null

      outputStream.writeObject(input)
      outputStream.flush()

      try
      {
        output = (OutputFromServerT) inputStream.readObject()
      } catch (EOFException e)
      {
        throw new DeviceNeedsRebootException(e)
      }

      log.trace("socket.close()")
      socket.close()

    }
    catch (TcpServerUnreachableException e)
    {
      throw e
    }
    catch (Throwable t)
    {
      throw new DeviceException("SerializableTCPClient has thrown a ${t.class.simpleName} while querying server. " +
        "Requesting to stop further apk explorations.", t, true)
    }

    return output
  }

  private Socket tryGetSocket(String serverAddress, int port) throws DeviceNeedsRebootException
  {
    Socket socket
    try
    {
      socket = new Socket(serverAddress, port)
    } catch (ConnectException e)
    {
      throw new DeviceNeedsRebootException(e)
    }
    return socket
  }
}
