/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.jdbc;

import org.apache.guacamole.auth.jdbc.user.UserContext;
import org.apache.guacamole.auth.jdbc.connectiongroup.RootConnectionGroup;
import org.apache.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupDirectory;
import org.apache.guacamole.auth.jdbc.connection.ConnectionDirectory;
import org.apache.guacamole.auth.jdbc.connection.ModeledGuacamoleConfiguration;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
import org.apache.guacamole.auth.jdbc.permission.SystemPermissionSet;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.auth.jdbc.user.UserDirectory;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupMapper;
import org.apache.guacamole.auth.jdbc.connection.ConnectionMapper;
import org.apache.guacamole.auth.jdbc.connection.ConnectionRecordMapper;
import org.apache.guacamole.auth.jdbc.permission.SystemPermissionMapper;
import org.apache.guacamole.auth.jdbc.user.UserMapper;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupService;
import org.apache.guacamole.auth.jdbc.connection.ConnectionService;
import org.apache.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.apache.guacamole.auth.jdbc.security.PasswordEncryptionService;
import org.apache.guacamole.auth.jdbc.security.SHA256PasswordEncryptionService;
import org.apache.guacamole.auth.jdbc.security.SaltService;
import org.apache.guacamole.auth.jdbc.security.SecureRandomSaltService;
import org.apache.guacamole.auth.jdbc.permission.SystemPermissionService;
import org.apache.guacamole.auth.jdbc.user.UserService;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.guacamole.auth.jdbc.permission.ConnectionGroupPermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.ConnectionGroupPermissionService;
import org.apache.guacamole.auth.jdbc.permission.ConnectionGroupPermissionSet;
import org.apache.guacamole.auth.jdbc.permission.ConnectionPermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.ConnectionPermissionService;
import org.apache.guacamole.auth.jdbc.permission.ConnectionPermissionSet;
import org.apache.guacamole.auth.jdbc.permission.UserPermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.UserPermissionService;
import org.apache.guacamole.auth.jdbc.permission.UserPermissionSet;
import org.apache.guacamole.auth.jdbc.activeconnection.ActiveConnectionDirectory;
import org.apache.guacamole.auth.jdbc.activeconnection.ActiveConnectionPermissionService;
import org.apache.guacamole.auth.jdbc.activeconnection.ActiveConnectionPermissionSet;
import org.apache.guacamole.auth.jdbc.activeconnection.ActiveConnectionService;
import org.apache.guacamole.auth.jdbc.activeconnection.TrackedActiveConnection;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterMapper;
import org.apache.guacamole.auth.jdbc.permission.SharingProfilePermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.SharingProfilePermissionService;
import org.apache.guacamole.auth.jdbc.permission.SharingProfilePermissionSet;
import org.apache.guacamole.auth.jdbc.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileDirectory;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileMapper;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileParameterMapper;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileService;
import org.apache.guacamole.auth.jdbc.tunnel.RestrictedGuacamoleTunnelService;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.mybatis.guice.MyBatisModule;
import org.mybatis.guice.datasource.builtin.PooledDataSourceProvider;

/**
 * Guice module which configures the injections used by the JDBC authentication
 * provider base. This module MUST be included in the Guice injector, or
 * authentication providers based on JDBC will not function.
 *
 * @author Michael Jumper
 * @author James Muehlner
 */
public class JDBCAuthenticationProviderModule extends MyBatisModule {

    /**
     * The environment of the Guacamole server.
     */
    private final JDBCEnvironment environment;

    /**
     * The AuthenticationProvider which is using this module to configure
     * injection.
     */
    private final AuthenticationProvider authProvider;

    /**
     * Creates a new JDBC authentication provider module that configures the
     * various injected base classes using the given environment, and provides
     * connections using the given socket service.
     *
     * @param authProvider
     *     The AuthenticationProvider which is using this module to configure
     *     injection.
     *
     * @param environment
     *     The environment to use to configure injected classes.
     */
    public JDBCAuthenticationProviderModule(AuthenticationProvider authProvider,
            JDBCEnvironment environment) {
        this.authProvider = authProvider;
        this.environment = environment;
    }

    @Override
    protected void initialize() {
        
        // Datasource
        bindDataSourceProviderType(PooledDataSourceProvider.class);
        
        // Transaction factory
        bindTransactionFactoryType(JdbcTransactionFactory.class);
        
        // Add MyBatis mappers
        addMapperClass(ConnectionMapper.class);
        addMapperClass(ConnectionGroupMapper.class);
        addMapperClass(ConnectionGroupPermissionMapper.class);
        addMapperClass(ConnectionPermissionMapper.class);
        addMapperClass(ConnectionRecordMapper.class);
        addMapperClass(ConnectionParameterMapper.class);
        addMapperClass(SystemPermissionMapper.class);
        addMapperClass(SharingProfileMapper.class);
        addMapperClass(SharingProfileParameterMapper.class);
        addMapperClass(SharingProfilePermissionMapper.class);
        addMapperClass(UserMapper.class);
        addMapperClass(UserPermissionMapper.class);
        
        // Bind core implementations of guacamole-ext classes
        bind(ActiveConnectionDirectory.class);
        bind(ActiveConnectionPermissionSet.class);
        bind(AuthenticationProvider.class).toInstance(authProvider);
        bind(JDBCEnvironment.class).toInstance(environment);
        bind(ConnectionDirectory.class);
        bind(ConnectionGroupDirectory.class);
        bind(ConnectionGroupPermissionSet.class);
        bind(ConnectionPermissionSet.class);
        bind(ModeledConnection.class);
        bind(ModeledConnectionGroup.class);
        bind(ModeledGuacamoleConfiguration.class);
        bind(ModeledSharingProfile.class);
        bind(ModeledUser.class);
        bind(RootConnectionGroup.class);
        bind(SharingProfileDirectory.class);
        bind(SharingProfilePermissionSet.class);
        bind(SystemPermissionSet.class);
        bind(TrackedActiveConnection.class);
        bind(UserContext.class);
        bind(UserDirectory.class);
        bind(UserPermissionSet.class);
        
        // Bind services
        bind(ActiveConnectionService.class);
        bind(ActiveConnectionPermissionService.class);
        bind(ConnectionGroupPermissionService.class);
        bind(ConnectionGroupService.class);
        bind(ConnectionPermissionService.class);
        bind(ConnectionService.class);
        bind(GuacamoleTunnelService.class).to(RestrictedGuacamoleTunnelService.class);
        bind(PasswordEncryptionService.class).to(SHA256PasswordEncryptionService.class);
        bind(SaltService.class).to(SecureRandomSaltService.class);
        bind(SharingProfilePermissionService.class);
        bind(SharingProfileService.class);
        bind(SystemPermissionService.class);
        bind(UserPermissionService.class);
        bind(UserService.class);
        
    }

}
